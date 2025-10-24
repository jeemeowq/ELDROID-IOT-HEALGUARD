const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();

const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: functions.config().gmail.email,
    pass: functions.config().gmail.password,
  },
});

exports.sendOTPEmail = functions.https.onCall(async (data, context) => {
  const {email, otp} = data;

  if (!email || !otp) {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "Email and OTP are required",
    );
  }

  const mailOptions = {
    from: "HealGuard <anjelymainit764@gmail.com>",
    to: email,
    subject: "HealGuard - Your OTP Code",
    html: `
    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
      <div style="background: #2196F3; padding: 20px; text-align: center;">
        <h1 style="color: white; margin: 0;">HealGuard</h1>
      </div>
      <div style="padding: 30px; background: #f9f9f9;">
        <h2 style="color: #333;">Your OTP Code</h2>
        <p style="color: #666;">Hello,</p>
        <p style="color: #666;">Your OTP code for password reset is:</p>
        <div style="text-align: center; margin: 30px 0;">
          <div style="background: white; padding: 20px; border-radius: 10px; 
          display: inline-block; border: 2px dashed #2196F3;">
            <h1 style="font-size: 32px; color: #333; margin: 0; 
            letter-spacing: 10px;">${otp}</h1>
          </div>
        </div>
        <p style="color: #666; font-size: 14px;">
          This code will expire in 5 minutes.
        </p>
        <p style="color: #999; font-size: 12px;">
          If you didn't request this OTP, please ignore this email.
        </p>
      </div>
      <div style="background: #333; padding: 15px; text-align: center;">
        <p style="color: white; margin: 0; font-size: 12px;">
          &copy; 2024 HealGuard. All rights reserved.
        </p>
      </div>
    </div>
    `,
  };

  try {
    await transporter.sendMail(mailOptions);
    console.log("OTP email sent to: " + email);
    return {success: true, message: "OTP sent successfully"};
  } catch (error) {
    console.error("Error sending OTP email:", error);
    throw new functions.https.HttpsError(
        "internal",
        "Failed to send OTP email",
    );
  }
});