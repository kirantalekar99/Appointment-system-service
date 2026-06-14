async function login(event) {
  event.preventDefault();

  const identifier = document.getElementById("loginIdentifier").value.trim();
  const passwordInput = document.getElementById("loginPassword") || document.getElementById("password");
  const password = passwordInput.value;

  if (!identifier) {
    showMessage("loginStatus", "Please enter your registered email or mobile number.", "error");
    return;
  }

  try {
    const response = await request("/auth/login", {
      method: "POST",
      body: JSON.stringify({ identifier, password })
    });

    saveSession(response);
    showMessage("loginStatus", "Login Successful", "success");

    setTimeout(() => {
      if (response.role === "ADMIN") {
        rememberToastForNextPage();
        window.location.href = "admin.html";
        return;
      }

      if (document.body.closest(".admin-auth-page")) {
        showMessage("loginStatus", "This page is for admin login only.", "error");
        clearSession();
        return;
      }
      goHome();
    }, 800);
  } catch (error) {
    showMessage("loginStatus", error.message, "error");
  }
}

async function submitForgotPassword(event) {
  event.preventDefault();

  const identifier = document.getElementById("forgotIdentifier").value.trim();
  const newPassword = document.getElementById("forgotNewPassword").value;
  const confirmPassword = document.getElementById("forgotConfirmPassword").value;

  if (!identifier) {
    showMessage("forgotPasswordStatus", "Enter your registered email or mobile number.", "error");
    return;
  }

  if (!validatePassword(newPassword)) {
    showMessage("forgotPasswordStatus", "Password must be at least 8 characters", "error");
    return;
  }

  if (newPassword !== confirmPassword) {
    showMessage("forgotPasswordStatus", "Password Mismatch", "error");
    return;
  }

  try {
    const response = await request("/auth/forgot-password", {
      method: "POST",
      body: JSON.stringify({ identifier, newPassword })
    });

    showMessage("forgotPasswordStatus", response || "Password Changed", "success");
    document.getElementById("forgotPasswordForm").reset();
    setTimeout(closeForgotPasswordPopup, 900);
  } catch (error) {
    showMessage("forgotPasswordStatus", error.message, "error");
  }
}

function clearAuthMessages() {
  ["loginStatus", "registerStatus"].forEach((id) => {
    const node = document.getElementById(id);
    if (!node) {
      return;
    }

    node.className = "message";
    node.textContent = "";
  });
}

function showLoginPanel() {
  document.getElementById("loginPanel")?.removeAttribute("hidden");
  document.getElementById("registerPanel")?.setAttribute("hidden", "");
  clearAuthMessages();
}

function showRegisterPanel() {
  document.getElementById("registerPanel")?.removeAttribute("hidden");
  document.getElementById("loginPanel")?.setAttribute("hidden", "");
  clearAuthMessages();
}

function openForgotPasswordPopup() {
  const popup = document.getElementById("forgotPasswordPopup");
  if (!popup) {
    return;
  }

  popup.classList.add("show");
  document.getElementById("forgotPasswordStatus").className = "message";
  document.getElementById("forgotPasswordStatus").textContent = "";
}

function closeForgotPasswordPopup() {
  const popup = document.getElementById("forgotPasswordPopup");
  if (!popup) {
    return;
  }

  popup.classList.remove("show");
}

document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("loginForm")?.addEventListener("submit", login);
  document.getElementById("openRegisterPanelButton")?.addEventListener("click", showRegisterPanel);
  document.getElementById("backToLoginButton")?.addEventListener("click", showLoginPanel);
  document.getElementById("forgotPasswordLink")?.addEventListener("click", (event) => {
    event.preventDefault();
    openForgotPasswordPopup();
  });
  document.getElementById("closeForgotPasswordButton")?.addEventListener("click", closeForgotPasswordPopup);
  document.getElementById("cancelForgotPasswordButton")?.addEventListener("click", closeForgotPasswordPopup);
  document.getElementById("forgotPasswordForm")?.addEventListener("submit", submitForgotPassword);
});
