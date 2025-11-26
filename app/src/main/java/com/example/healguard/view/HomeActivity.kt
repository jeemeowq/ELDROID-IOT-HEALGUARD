package com.example.healguard.view

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import com.example.healguard.R
import com.example.healguard.broadcast.MedicineAlarmReceiver
import com.example.healguard.model.HistoryItem
import com.example.healguard.model.Medicine
import com.example.healguard.model.NotificationItem
import com.example.healguard.presenter.MedicinePresenter
import com.example.healguard.utils.PrefsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : Activity(), IHomeView {

    private lateinit var presenter: MedicinePresenter
    private lateinit var greetingText: TextView
    private lateinit var todayDateText: TextView
    private lateinit var todayDate: TextView
    private lateinit var medicinesContainer: LinearLayout
    private lateinit var profileIcon: ImageView
    private lateinit var hardwareStatusText: TextView
    private lateinit var hardwareStatusIndicator: ImageView

    private val medicineList = mutableListOf<Medicine>()
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private lateinit var prefsManager: PrefsManager
    private lateinit var alarmManager: AlarmManager
    private var notificationsDialog: AlertDialog? = null
    private var historyDialog: AlertDialog? = null
    private var hardwareStatusListener: ValueEventListener? = null
    private var medicinesListener: ValueEventListener? = null

    companion object {
        private const val REQUEST_CODE_PROFILE = 1001
        private const val ALARM_REQUEST_CODE = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        prefsManager = PrefsManager(this)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        greetingText = findViewById(R.id.greetingText)
        todayDateText = findViewById(R.id.todayDateText)
        todayDate = findViewById(R.id.todayDate)
        medicinesContainer = findViewById(R.id.medicinesContainer)
        profileIcon = findViewById(R.id.profileIcon)
        hardwareStatusText = findViewById(R.id.hardwareStatusText)
        hardwareStatusIndicator = findViewById(R.id.hardwareStatusIndicator)

        updateGreetingText()

        setupHeaderActions()
        setupBottomNavigation()
        setupRealTimeDate()
        setupMedicineLongPress()
        setupFirebaseListeners()

        presenter = MedicinePresenter(this)
        scheduleAllMedicineAlarms()

        setUserAsActive()
    }

    private fun setupFirebaseListeners() {
        currentUser?.uid?.let { userId ->
            // Hardware status listener
            val hardwareStatusRef = database.getReference("users/$userId/hardware_status")
            hardwareStatusListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.getValue(String::class.java)
                    status?.let { updateHardwareStatus(it) }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HardwareStatus", "Listen failed", error.toException())
                }
            }
            hardwareStatusRef.addValueEventListener(hardwareStatusListener!!)

            // Medicines real-time listener
            val medicinesRef = database.getReference("users/$userId/medicines")
            medicinesListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    medicineList.clear()
                    for (medicineSnapshot in snapshot.children) {
                        val medicine = medicineSnapshot.getValue(Medicine::class.java)
                        medicine?.let {
                            it.id = medicineSnapshot.key ?: ""
                            medicineList.add(it)
                        }
                    }
                    showMedicines(medicineList)
                    scheduleAllMedicineAlarms()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MedicineSync", "Failed to load medicines", error.toException())
                }
            }
            medicinesRef.addValueEventListener(medicinesListener!!)
        }
    }

    private fun setUserAsActive() {
        currentUser?.uid?.let { userId ->
            val activeUserRef = database.getReference("active_user")
            activeUserRef.setValue(userId)
                .addOnSuccessListener {
                    Log.d("Firebase", "User set as active in RTDB: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Failed to set active user", e)
                }
        }
    }

    private fun updateHardwareStatus(status: String) {
        runOnUiThread {
            when {
                status.contains("ALARM", ignoreCase = true) -> {
                    hardwareStatusText.text = "Hardware: Alarm Active"
                    hardwareStatusIndicator.setColorFilter(resources.getColor(R.color.red))
                }
                status.contains("READY", ignoreCase = true) -> {
                    hardwareStatusText.text = "Hardware: Ready"
                    hardwareStatusIndicator.setColorFilter(resources.getColor(R.color.green))
                }
                status.contains("OFFLINE", ignoreCase = true) -> {
                    hardwareStatusText.text = "Hardware: Offline"
                    hardwareStatusIndicator.setColorFilter(resources.getColor(R.color.gray))
                }
                else -> {
                    hardwareStatusText.text = "Hardware: $status"
                    hardwareStatusIndicator.setColorFilter(resources.getColor(R.color.blue))
                }
            }
        }
    }

    private fun updateGreetingText() {
        val username = prefsManager.getUsername()
        greetingText.text = "Hi, $username!"
    }

    override fun onResume() {
        super.onResume()
        updateGreetingText()
    }

    override fun onPause() {
        super.onPause()
        notificationsDialog?.dismiss()
        historyDialog?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        hardwareStatusListener?.let {
            currentUser?.uid?.let { userId ->
                database.getReference("users/$userId/hardware_status").removeEventListener(it)
            }
        }
        medicinesListener?.let {
            currentUser?.uid?.let { userId ->
                database.getReference("users/$userId/medicines").removeEventListener(it)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun setupRealTimeDate() {
        val phTimeZone = TimeZone.getTimeZone("Asia/Manila")
        val calendar = Calendar.getInstance(phTimeZone)

        val dateFormat = SimpleDateFormat("MMMM dd", Locale.getDefault()).apply {
            timeZone = phTimeZone
        }
        val dayOfWeekFormat = SimpleDateFormat("E", Locale.getDefault()).apply {
            timeZone = phTimeZone
        }
        val dayOfMonthFormat = SimpleDateFormat("d", Locale.getDefault()).apply {
            timeZone = phTimeZone
        }

        todayDate.text = "Today, ${dateFormat.format(calendar.time)}"
        todayDateText.text = "${dayOfMonthFormat.format(calendar.time)}\n${dayOfWeekFormat.format(calendar.time)}"

        val nextDates = listOf(
            R.id.nextDate1,
            R.id.nextDate2,
            R.id.nextDate3,
            R.id.nextDate4
        )

        for (i in 0 until 4) {
            val nextCalendar = Calendar.getInstance(phTimeZone)
            nextCalendar.add(Calendar.DAY_OF_YEAR, i + 1)
            val dateView = findViewById<TextView>(nextDates[i])
            dateView.text = "${dayOfMonthFormat.format(nextCalendar.time)}\n${dayOfWeekFormat.format(nextCalendar.time)}"
        }
    }

    private fun setupMedicineLongPress() {
        medicinesContainer.setOnLongClickListener {
            false
        }
    }

    override fun showMedicines(medicines: List<Medicine>) {
        medicinesContainer.removeAllViews()

        if (medicines.isEmpty()) {
            showEmptyState()
            return
        }

        medicines.forEachIndexed { index, medicine ->
            val cardView = LayoutInflater.from(this)
                .inflate(R.layout.item_medicine_card, medicinesContainer, false)

            val medName = cardView.findViewById<TextView>(R.id.medName)
            val medInstruction = cardView.findViewById<TextView>(R.id.medInstruction)
            val medTime = cardView.findViewById<TextView>(R.id.medTime)
            val medIcon = cardView.findViewById<ImageView>(R.id.medIcon)

            medName.text = medicine.name
            medInstruction.text = "${medicine.usage}, ${medicine.description}"
            medTime.text = formatTimeTo12Hour(medicine.time ?: "Set time")
            setMedicineIcon(medIcon, medicine.type)

            cardView.setOnLongClickListener {
                showEditDeleteDialog(index, medicine)
                true
            }

            medicinesContainer.addView(cardView)
        }
    }

    private fun setMedicineIcon(imageView: ImageView, type: String?) {
        when (type?.lowercase()) {
            "tablet" -> imageView.setImageResource(R.drawable.tableticon)
            "capsule" -> imageView.setImageResource(R.drawable.capsuleicon)
            "liquid" -> imageView.setImageResource(R.drawable.liquidmedicon)
            "cream" -> imageView.setImageResource(R.drawable.creamicon)
            "patch" -> imageView.setImageResource(R.drawable.patchesicon)
            "spray" -> imageView.setImageResource(R.drawable.sprayicon)
            else -> imageView.setImageResource(R.drawable.capsuleicon)
        }
    }

    private fun showEditDeleteDialog(index: Int, medicine: Medicine) {
        val options = arrayOf("Edit", "Delete", "Send to Hardware")

        AlertDialog.Builder(this)
            .setTitle("Medicine Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditMedicineDialog(index, medicine)
                    1 -> showDeleteConfirmationDialog(index, medicine)
                    2 -> sendMedicineToHardware(medicine)
                }
            }
            .show()
    }

    private fun sendMedicineToHardware(medicine: Medicine) {
        // This will automatically sync via Realtime Database now
        Toast.makeText(this, "Medicine synced with hardware", Toast.LENGTH_SHORT).show()
        saveSuccessNotification("sent_to_hardware", medicine)
    }

    private fun showEditMedicineDialog(index: Int, medicine: Medicine) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.medicine_schedule_dialog, null)

        val medNameInput = dialogView.findViewById<EditText>(R.id.medNameInput)
        val medUsageInput = dialogView.findViewById<EditText>(R.id.medUsageInput)
        val medDescInput = dialogView.findViewById<EditText>(R.id.medDescInput)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)

        val tabletsCheckbox = dialogView.findViewById<CheckBox>(R.id.tabletsCheckbox)
        val capsulesCheckbox = dialogView.findViewById<CheckBox>(R.id.capsulesCheckbox)
        val liquidMedsCheckbox = dialogView.findViewById<CheckBox>(R.id.liquidMedsCheckbox)
        val creamsCheckbox = dialogView.findViewById<CheckBox>(R.id.creamsCheckbox)
        val patchesCheckbox = dialogView.findViewById<CheckBox>(R.id.patchesCheckbox)
        val spraysCheckbox = dialogView.findViewById<CheckBox>(R.id.spraysCheckbox)

        medNameInput.setText(medicine.name)
        medUsageInput.setText(medicine.usage)
        medDescInput.setText(medicine.description)

        when (medicine.type?.lowercase()) {
            "tablet" -> tabletsCheckbox.isChecked = true
            "capsule" -> capsulesCheckbox.isChecked = true
            "liquid" -> liquidMedsCheckbox.isChecked = true
            "cream" -> creamsCheckbox.isChecked = true
            "patch" -> patchesCheckbox.isChecked = true
            "spray" -> spraysCheckbox.isChecked = true
        }

        medicine.time?.let { timeString ->
            if (timeString.contains(":")) {
                val parts = timeString.split(":")
                if (parts.size == 2) {
                    timePicker.currentHour = parts[0].toInt()
                    timePicker.currentMinute = parts[1].toInt()
                }
            }
        }

        timePicker.setIs24HourView(false)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnOk.setOnClickListener {
            if (!validateMedicineInput(medNameInput, medUsageInput, tabletsCheckbox, capsulesCheckbox,
                    liquidMedsCheckbox, creamsCheckbox, patchesCheckbox, spraysCheckbox)) {
                return@setOnClickListener
            }

            val selectedDosageForm = when {
                tabletsCheckbox.isChecked -> "tablet"
                capsulesCheckbox.isChecked -> "capsule"
                liquidMedsCheckbox.isChecked -> "liquid"
                creamsCheckbox.isChecked -> "cream"
                patchesCheckbox.isChecked -> "patch"
                spraysCheckbox.isChecked -> "spray"
                else -> "capsule"
            }

            val hour = timePicker.currentHour
            val minute = timePicker.currentMinute
            val timeString = String.format("%02d:%02d", hour, minute)

            val updatedMedicine = Medicine(
                id = medicine.id,
                name = medNameInput.text.toString().trim(),
                usage = medUsageInput.text.toString().trim(),
                description = medDescInput.text.toString().trim(),
                time = timeString,
                type = selectedDosageForm,
                timing = medicine.timing
            )

            medicineList[index] = updatedMedicine

            updateMedicineInFirebase(updatedMedicine)
            saveSuccessNotification("edited", updatedMedicine)
            saveMedicineHistory("edited", updatedMedicine)
            cancelMedicineAlarm(medicine.id)
            scheduleMedicineAlarm(updatedMedicine)
            showMedicines(medicineList)
            Toast.makeText(this, "Medicine updated successfully", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(index: Int, medicine: Medicine) {
        AlertDialog.Builder(this)
            .setTitle("Delete Medicine")
            .setMessage("Are you sure you want to delete ${medicine.name}?")
            .setPositiveButton("Delete") { _, _ ->
                if (medicine.id.isNullOrEmpty()) {
                    Toast.makeText(this, "Error: Invalid medicine ID", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                medicineList.removeAt(index)
                deleteMedicineFromFirebase(medicine.id)
                saveSuccessNotification("deleted", medicine)
                saveMedicineHistory("deleted", medicine)
                cancelMedicineAlarm(medicine.id)
                showMedicines(medicineList)
                Toast.makeText(this, "Medicine deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatTimeTo12Hour(timeString: String): String {
        return try {
            if (timeString.contains(":")) {
                val parts = timeString.split(":")
                if (parts.size == 2) {
                    var hour = parts[0].toInt()
                    val minute = parts[1].toInt()
                    val amPm = if (hour < 12) "AM" else "PM"
                    hour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                    String.format("%d:%02d %s", hour, minute, amPm)
                } else {
                    timeString
                }
            } else {
                timeString
            }
        } catch (e: Exception) {
            timeString
        }
    }

    private fun showEmptyState() {
        val emptyText = TextView(this).apply {
            text = "No Medicine Schedule Yet..."
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.darker_gray))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 40, 0, 40)
        }
        medicinesContainer.addView(emptyText)
    }

    private fun setupHeaderActions() {
        findViewById<LinearLayout>(R.id.datePickerLayout).setOnClickListener {
            val phTimeZone = TimeZone.getTimeZone("Asia/Manila")
            val calendar = Calendar.getInstance(phTimeZone)

            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val chosen = Calendar.getInstance(phTimeZone)
                    chosen.set(year, month, dayOfMonth)
                    val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).apply {
                        timeZone = phTimeZone
                    }
                    todayDate.text = "Today, ${sdf.format(chosen.time)}"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
        findViewById<ImageView>(R.id.notificationIcon).setOnClickListener {
            showNotificationsDialog()
        }
        profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_PROFILE)
        }

        hardwareStatusText.setOnClickListener {
            refreshHardwareStatus()
        }
    }

    private fun refreshHardwareStatus() {
        currentUser?.uid?.let { userId ->
            database.getReference("users/$userId/hardware_status").get()
                .addOnSuccessListener { snapshot ->
                    val status = snapshot.getValue(String::class.java)
                    status?.let { updateHardwareStatus(it) }
                }
        }
        Toast.makeText(this, "Refreshing hardware status...", Toast.LENGTH_SHORT).show()
    }

    private fun setupBottomNavigation() {
        val plusIcon = findViewById<ImageView>(R.id.plusIcon)
        val historyIcon = findViewById<ImageView>(R.id.historyIcon)
        val homeIcon = findViewById<ImageView>(R.id.homeIcon)

        homeIcon.setOnClickListener {
            showMedicines(medicineList)
            updateGreetingText()
            Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show()
        }

        plusIcon.setOnClickListener {
            showAddMedicineDialog()
        }

        historyIcon.setOnClickListener {
            showHistoryDialog()
        }
    }

    private fun formatDateTime(timestamp: Long): String {
        val phTimeZone = TimeZone.getTimeZone("Asia/Manila")
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).apply {
            timeZone = phTimeZone
        }
        val timeFormat = SimpleDateFormat("h:mma", Locale.getDefault()).apply {
            timeZone = phTimeZone
        }

        val date = Date(timestamp)
        val formattedDate = dateFormat.format(date)
        val formattedTime = timeFormat.format(date)

        return "$formattedDate & $formattedTime"
    }

    private fun showNotificationsDialog() {
        try {
            val scrollView = ScrollView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                isVerticalScrollBarEnabled = true
            }

            val mainContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
            }

            val title = TextView(this).apply {
                text = "Notifications"
                textSize = 18f
                setTextColor(resources.getColor(R.color.black))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
            }
            mainContainer.addView(title)

            val notificationsContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
            }
            mainContainer.addView(notificationsContainer)

            scrollView.addView(mainContainer)

            notificationsDialog = AlertDialog.Builder(this)
                .setView(scrollView)
                .setCancelable(true)
                .create()

            notificationsDialog?.setCanceledOnTouchOutside(true)

            loadNotificationsFromFirebase(notificationsContainer)

            notificationsDialog?.show()

            notificationsDialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resources.displayMetrics.heightPixels * 2 / 3
            )

        } catch (e: Exception) {
            Toast.makeText(this, "Error showing notifications", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun loadNotificationsFromFirebase(container: LinearLayout) {
        currentUser?.uid?.let { userId ->
            database.getReference("users/$userId/notifications")
                .orderByChild("timestamp")
                .limitToLast(10)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        container.removeAllViews()

                        if (!snapshot.exists()) {
                            showEmptyNotifications(container)
                            return
                        }

                        val notifications = mutableListOf<NotificationItem>()
                        for (notificationSnapshot in snapshot.children) {
                            val notification = notificationSnapshot.getValue(NotificationItem::class.java)
                            notification?.let {
                                it.id = notificationSnapshot.key ?: ""
                                notifications.add(it)
                            }
                        }

                        // Sort by timestamp descending
                        notifications.sortByDescending { it.timestamp }

                        if (notifications.isEmpty()) {
                            showEmptyNotifications(container)
                        } else {
                            for (notification in notifications) {
                                addNotificationToView(container, notification)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showEmptyNotifications(container)
                    }
                })
        } ?: showEmptyNotifications(container)
    }

    private fun addNotificationToView(container: LinearLayout, notification: NotificationItem) {
        val notificationView = LayoutInflater.from(this)
            .inflate(R.layout.notification_popup, container, false)

        val notificationText = notificationView.findViewById<TextView>(R.id.notificationText)
        val notificationDateTime = notificationView.findViewById<TextView>(R.id.notificationDate_Time)

        notificationText.text = notification.message
        notificationDateTime.text = formatDateTime(notification.timestamp)

        container.addView(notificationView)
    }

    private fun showEmptyNotifications(container: LinearLayout) {
        val emptyText = TextView(this).apply {
            text = "No notifications yet"
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.darker_gray))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 40, 0, 40)
        }
        container.addView(emptyText)
    }

    private fun showHistoryDialog() {
        try {
            val scrollView = ScrollView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                isVerticalScrollBarEnabled = true
            }

            val mainContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
            }

            val title = TextView(this).apply {
                text = "History"
                textSize = 18f
                setTextColor(resources.getColor(R.color.black))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
            }
            mainContainer.addView(title)

            val historyContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
            }
            mainContainer.addView(historyContainer)

            scrollView.addView(mainContainer)

            historyDialog = AlertDialog.Builder(this)
                .setView(scrollView)
                .setCancelable(true)
                .create()

            historyDialog?.setCanceledOnTouchOutside(true)

            loadHistoryFromFirebase(historyContainer)

            historyDialog?.show()

            historyDialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resources.displayMetrics.heightPixels * 2 / 3
            )

        } catch (e: Exception) {
            Toast.makeText(this, "Error showing history", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun loadHistoryFromFirebase(container: LinearLayout) {
        currentUser?.uid?.let { userId ->
            database.getReference("users/$userId/history")
                .orderByChild("timestamp")
                .limitToLast(20)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        container.removeAllViews()

                        if (!snapshot.exists()) {
                            showEmptyHistory(container)
                            return
                        }

                        val historyItems = mutableListOf<HistoryItem>()
                        for (historySnapshot in snapshot.children) {
                            val historyItem = historySnapshot.getValue(HistoryItem::class.java)
                            historyItem?.let {
                                it.id = historySnapshot.key ?: ""
                                historyItems.add(it)
                            }
                        }

                        // Sort by timestamp descending
                        historyItems.sortByDescending { it.timestamp }

                        if (historyItems.isEmpty()) {
                            showEmptyHistory(container)
                        } else {
                            for (historyItem in historyItems) {
                                addHistoryToView(container, historyItem)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showEmptyHistory(container)
                    }
                })
        } ?: showEmptyHistory(container)
    }

    private fun addHistoryToView(container: LinearLayout, historyItem: HistoryItem) {
        val historyView = LayoutInflater.from(this)
            .inflate(R.layout.history_popup, container, false)

        val historyText = historyView.findViewById<TextView>(R.id.historyText)
        val historyDateTime = historyView.findViewById<TextView>(R.id.historyDate_Time)

        historyText.text = historyItem.message
        historyDateTime.text = formatDateTime(historyItem.timestamp)

        container.addView(historyView)
    }

    private fun showEmptyHistory(container: LinearLayout) {
        val emptyText = TextView(this).apply {
            text = "No history yet"
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.darker_gray))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 40, 0, 40)
        }
        container.addView(emptyText)
    }

    private fun showAddMedicineDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.medicine_schedule_dialog, null)

        val medNameInput = dialogView.findViewById<EditText>(R.id.medNameInput)
        val medUsageInput = dialogView.findViewById<EditText>(R.id.medUsageInput)
        val medDescInput = dialogView.findViewById<EditText>(R.id.medDescInput)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)

        val tabletsCheckbox = dialogView.findViewById<CheckBox>(R.id.tabletsCheckbox)
        val capsulesCheckbox = dialogView.findViewById<CheckBox>(R.id.capsulesCheckbox)
        val liquidMedsCheckbox = dialogView.findViewById<CheckBox>(R.id.liquidMedsCheckbox)
        val creamsCheckbox = dialogView.findViewById<CheckBox>(R.id.creamsCheckbox)
        val patchesCheckbox = dialogView.findViewById<CheckBox>(R.id.patchesCheckbox)
        val spraysCheckbox = dialogView.findViewById<CheckBox>(R.id.spraysCheckbox)

        timePicker.setIs24HourView(false)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnOk.setOnClickListener {
            if (!validateMedicineInput(medNameInput, medUsageInput, tabletsCheckbox, capsulesCheckbox,
                    liquidMedsCheckbox, creamsCheckbox, patchesCheckbox, spraysCheckbox)) {
                return@setOnClickListener
            }

            val selectedDosageForm = when {
                tabletsCheckbox.isChecked -> "tablet"
                capsulesCheckbox.isChecked -> "capsule"
                liquidMedsCheckbox.isChecked -> "liquid"
                creamsCheckbox.isChecked -> "cream"
                patchesCheckbox.isChecked -> "patch"
                spraysCheckbox.isChecked -> "spray"
                else -> "capsule"
            }

            val hour = timePicker.currentHour
            val minute = timePicker.currentMinute
            val timeString = String.format("%02d:%02d", hour, minute)

            val medicineId = UUID.randomUUID().toString()
            val medicine = Medicine(
                id = medicineId,
                name = medNameInput.text.toString().trim(),
                usage = medUsageInput.text.toString().trim(),
                description = medDescInput.text.toString().trim(),
                time = timeString,
                type = selectedDosageForm,
                timing = "As directed"
            )

            medicineList.add(medicine)

            saveMedicineToFirebase(medicine)
            saveSuccessNotification("added", medicine)
            saveMedicineHistory("added", medicine)
            scheduleMedicineAlarm(medicine)

            showMedicines(medicineList)
            Toast.makeText(this, "Medicine added successfully", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun calculateTimeDifference(medicineTime: String): String {
        return try {
            val phTimeZone = TimeZone.getTimeZone("Asia/Manila")
            val currentCalendar = Calendar.getInstance(phTimeZone)
            val medicineCalendar = Calendar.getInstance(phTimeZone)

            val timeParts = medicineTime.split(":")
            if (timeParts.size != 2) return ""

            val medicineHour = timeParts[0].toInt()
            val medicineMinute = timeParts[1].toInt()

            medicineCalendar.set(Calendar.HOUR_OF_DAY, medicineHour)
            medicineCalendar.set(Calendar.MINUTE, medicineMinute)
            medicineCalendar.set(Calendar.SECOND, 0)

            if (medicineCalendar.timeInMillis <= currentCalendar.timeInMillis) {
                medicineCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val differenceMillis = medicineCalendar.timeInMillis - currentCalendar.timeInMillis

            val hours = differenceMillis / (1000 * 60 * 60)
            val minutes = (differenceMillis % (1000 * 60 * 60)) / (1000 * 60)

            when {
                hours > 0 && minutes > 0 -> "${hours}hrs and ${minutes}mins"
                hours > 0 -> "${hours}hrs"
                minutes > 0 -> "${minutes}mins"
                else -> "less than a minute"
            }
        } catch (e: Exception) {
            Log.e("TimeCalc", "Error calculating time difference: ${e.message}")
            ""
        }
    }

    private fun saveSuccessNotification(action: String, medicine: Medicine) {
        val timeDifference = calculateTimeDifference(medicine.time ?: "")

        val message = when (action) {
            "added" -> "You've been successfully added ${medicine.name}. Reminder it will ring in $timeDifference."
            "edited" -> "You've been successfully edited ${medicine.name}. Reminder it will ring in $timeDifference."
            "deleted" -> "You've been successfully deleted ${medicine.name}"
            "sent_to_hardware" -> "Medicine ${medicine.name} sent to hardware device"
            else -> "Medicine action completed for ${medicine.name}"
        }

        val successNotification = NotificationItem(
            type = "success",
            message = message,
            medicineName = medicine.name,
            dosage = medicine.usage,
            time = medicine.time ?: "",
            timestamp = System.currentTimeMillis()
        )

        saveNotificationToFirebase(successNotification)
    }

    private fun saveMedicineHistory(action: String, medicine: Medicine) {
        val message = when (action) {
            "added" -> "You been successfully added ${medicine.name}"
            "edited" -> "You been successfully edited ${medicine.name}"
            "deleted" -> "You been successfully deleted ${medicine.name}"
            else -> "Medicine action performed on ${medicine.name}"
        }

        val historyItem = HistoryItem(
            action = action,
            medicineName = medicine.name,
            dosage = medicine.usage,
            message = message,
            timestamp = System.currentTimeMillis()
        )

        currentUser?.uid?.let { userId ->
            database.getReference("users/$userId/history/${historyItem.id}")
                .setValue(historyItem)
                .addOnSuccessListener {
                    Log.d("Firebase", "History saved successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error saving history: ${e.message}")
                }
        } ?: run {
            Log.e("Firebase", "User not logged in, cannot save history")
        }
    }

    private fun validateMedicineInput(
        medNameInput: EditText,
        medUsageInput: EditText,
        tabletsCheckbox: CheckBox,
        capsulesCheckbox: CheckBox,
        liquidMedsCheckbox: CheckBox,
        creamsCheckbox: CheckBox,
        patchesCheckbox: CheckBox,
        spraysCheckbox: CheckBox
    ): Boolean {
        val medicineName = medNameInput.text.toString().trim()
        if (medicineName.isEmpty()) {
            medNameInput.error = "Please enter medicine name"
            Toast.makeText(this, "Medicine name cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }

        val medicineUsage = medUsageInput.text.toString().trim()
        if (medicineUsage.isEmpty()) {
            medUsageInput.error = "Please enter dosage"
            Toast.makeText(this, "Dosage cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }

        val isDosageFormSelected = tabletsCheckbox.isChecked ||
                capsulesCheckbox.isChecked ||
                liquidMedsCheckbox.isChecked ||
                creamsCheckbox.isChecked ||
                patchesCheckbox.isChecked ||
                spraysCheckbox.isChecked

        if (!isDosageFormSelected) {
            Toast.makeText(this, "Please select at least one dosage form", Toast.LENGTH_SHORT).show()
            return false
        }

        val selectedCount = listOf(
            tabletsCheckbox.isChecked,
            capsulesCheckbox.isChecked,
            liquidMedsCheckbox.isChecked,
            creamsCheckbox.isChecked,
            patchesCheckbox.isChecked,
            spraysCheckbox.isChecked
        ).count { it }

        if (selectedCount > 1) {
            Toast.makeText(this, "Please select only one dosage form", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun loadMedicinesFromFirebase() {
        // This is now handled by the real-time listener in setupFirebaseListeners()
    }

    private fun saveMedicineToFirebase(medicine: Medicine) {
        currentUser?.uid?.let { userId ->
            val medicineId = medicine.id ?: UUID.randomUUID().toString()
            val userMedicinesRef = database.getReference("users/$userId/medicines/$medicineId")

            userMedicinesRef.setValue(medicine)
                .addOnSuccessListener {
                    Log.d("Firebase", "Medicine saved to RTDB")
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save medicine", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateMedicineInFirebase(medicine: Medicine) {
        currentUser?.uid?.let { userId ->
            if (medicine.id.isNullOrEmpty()) {
                Toast.makeText(this, "Error: Invalid medicine ID", Toast.LENGTH_SHORT).show()
                return
            }

            database.getReference("users/$userId/medicines/${medicine.id}")
                .setValue(medicine)
                .addOnSuccessListener {
                    Log.d("Firebase", "Medicine updated successfully")
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update medicine", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteMedicineFromFirebase(medicineId: String?) {
        if (medicineId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Invalid medicine ID", Toast.LENGTH_SHORT).show()
            return
        }

        currentUser?.uid?.let { userId ->
            database.getReference("users/$userId/medicines/$medicineId")
                .removeValue()
                .addOnSuccessListener {
                    Log.d("Firebase", "Medicine deleted successfully")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to delete medicine: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun scheduleAllMedicineAlarms() {
        for (medicine in medicineList) {
            scheduleMedicineAlarm(medicine)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleMedicineAlarm(medicine: Medicine) {
        try {
            val medicineTime = medicine.time ?: return

            val timeParts = medicineTime.split(":")
            if (timeParts.size != 2) return

            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()

            val phTimeZone = TimeZone.getTimeZone("Asia/Manila")
            val calendar = Calendar.getInstance(phTimeZone).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)

                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val intent = Intent(this, MedicineAlarmReceiver::class.java).apply {
                putExtra("medicine_name", medicine.name)
                putExtra("medicine_dosage", medicine.usage)
                putExtra("medicine_id", medicine.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                ALARM_REQUEST_CODE + (medicine.id?.hashCode() ?: 0),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )

            val notificationMessage = "Medicine reminder: ${medicine.name}, ${medicine.usage} at ${formatTimeTo12Hour(medicineTime)}"
            saveNotificationToFirebase(
                NotificationItem(
                    type = "scheduled",
                    message = notificationMessage,
                    medicineName = medicine.name,
                    dosage = medicine.usage,
                    time = medicineTime,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: SecurityException) {
            Log.e("AlarmError", "Cannot schedule exact alarm: ${e.message}")
            Toast.makeText(this, "Cannot set exact alarm. Please check app permissions.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("AlarmError", "Error scheduling alarm: ${e.message}")
            Toast.makeText(this, "Error setting medicine reminder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelMedicineAlarm(medicineId: String?) {
        if (medicineId.isNullOrEmpty()) return

        val intent = Intent(this, MedicineAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE + medicineId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun saveNotificationToFirebase(notification: NotificationItem) {
        currentUser?.uid?.let { userId ->
            database.getReference("users/$userId/notifications/${notification.id}")
                .setValue(notification)
                .addOnSuccessListener {
                    Log.d("Firebase", "Notification saved successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error saving notification: ${e.message}")
                }
        } ?: run {
            Log.e("Firebase", "User not logged in, cannot save notification")
        }
    }
}