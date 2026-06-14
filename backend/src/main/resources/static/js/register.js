let pendingRegisterEmail = "";

async function register(event) {
  event.preventDefault();

  const role = document.getElementById("accountRole").value;
  const phone = document.getElementById("phone").value.trim();
  const email = document.getElementById("email").value.trim();
  const password = document.getElementById("password").value;

  if (!validatePhone(phone)) {
    showMessage("registerStatus", "Phone number must contain exactly 10 digits.", "error");
    return;
  }

  if (!validateEmail(email)) {
    showMessage("registerStatus", "Please enter a valid email address.", "error");
    return;
  }

  if (!validatePassword(password)) {
    showMessage("registerStatus", "Password must be at least 8 characters and include 1 number and 1 special character.", "error");
    return;
  }

  const payload = {
    name: document.getElementById("name").value.trim(),
    email,
    password,
    phone,
    role,
    specialization: document.getElementById("doctorSpecialtyInput").value.trim(),
    experience: document.getElementById("doctorExperienceInput").value ? Number(document.getElementById("doctorExperienceInput").value) : null,
    bio: document.getElementById("doctorBioInput").value.trim(),
    availableDays: document.getElementById("doctorAvailableDaysInput").value.trim()
  };

  try {
    const response = await request("/auth/register", {
      method: "POST",
      body: JSON.stringify(payload)
    });

    pendingRegisterEmail = email;
    showMessage("registerStatus", response || "OTP sent successfully.", "success");
    document.getElementById("registerOtpEmail").value = email;
    openRegisterOtpPopup();
  } catch (error) {
    showMessage("registerStatus", error.message, "error");
  }
}

async function verifyRegisterOtp(event) {
  event.preventDefault();

  const email = document.getElementById("registerOtpEmail").value.trim() || pendingRegisterEmail;
  const otp = document.getElementById("registerOtpCode").value.trim();

  if (!otp) {
    showMessage("registerOtpStatus", "Please enter the OTP sent to your email.", "error");
    return;
  }

  try {
    const response = await request("/auth/register/verify-otp", {
      method: "POST",
      body: JSON.stringify({ email, otp })
    });

    saveSession(response);
    showMessage("registerOtpStatus", "Registration successful. Welcome to CareBridge.", "success");

    setTimeout(() => {
      closeRegisterOtpPopup();
      goHome();
    }, 900);
  } catch (error) {
    showMessage("registerOtpStatus", error.message, "error");
  }
}

function openRegisterOtpPopup() {
  document.getElementById("registerOtpPopup")?.classList.add("show");
}

function closeRegisterOtpPopup() {
  document.getElementById("registerOtpPopup")?.classList.remove("show");
}

function toggleDoctorFields() {
  const role = document.getElementById("accountRole");
  const doctorFields = document.getElementById("doctorExtraFields");
  if (!role || !doctorFields) {
    return;
  }

  doctorFields.style.display = role.value === "DOCTOR" ? "grid" : "none";
}

document.addEventListener("DOMContentLoaded", () => {
  const roleSelect = document.getElementById("accountRole");
  const registerForm = document.getElementById("registerForm");
  if (!roleSelect || !registerForm) {
    return;
  }

  toggleDoctorFields();
  roleSelect.addEventListener("change", toggleDoctorFields);
  registerForm.addEventListener("submit", register);
  document.getElementById("registerOtpForm")?.addEventListener("submit", verifyRegisterOtp);
  document.getElementById("closeRegisterOtpButton")?.addEventListener("click", closeRegisterOtpPopup);
  document.getElementById("cancelRegisterOtpButton")?.addEventListener("click", closeRegisterOtpPopup);
});
