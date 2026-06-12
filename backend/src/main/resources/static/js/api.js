const BASE_URL = "/api";
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_PATTERN = /^\d{10}$/;
const PASSWORD_NUMBER_PATTERN = /\d/;
const PASSWORD_SPECIAL_PATTERN = /[^A-Za-z0-9]/;

async function request(path, options = {}) {
  const session = getSession();
  const headers = {
    "Content-Type": "application/json",
    ...(session ? {
      "X-User-Id": String(session.id),
      "X-User-Role": session.role
    } : {}),
    ...(options.headers || {})
  };

  const response = await fetch(`${BASE_URL}${path}`, {
    headers,
    ...options
  });

  if (!response.ok) {
    let message = "Something went wrong. Please try again.";

    try {
      const error = await response.json();
      message = error.message || error.error || message;
    } catch (err) {
      const text = await response.text();
      message = text || message;
    }

    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("content-type") || "";
  if (!contentType.includes("application/json")) {
    const text = await response.text();
    return text ? text : null;
  }

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

function saveSession(user) {
  localStorage.setItem("appointmentUser", JSON.stringify(user));
}

function getSession() {
  const value = localStorage.getItem("appointmentUser");
  return value ? JSON.parse(value) : null;
}

function clearSession() {
  localStorage.removeItem("appointmentUser");
}

function goHome() {
  window.location.replace("index.html");
}

function goLogin() {
  window.location.replace("user-login.html");
}

function showMessage(elementId, message, type) {
  const box = document.getElementById(elementId);
  if (!box) {
    return;
  }

  box.className = `message show ${type}`;
  box.textContent = message;
}

function updateUserArea() {
  const target = document.getElementById("headerUserArea");
  if (!target) {
    return;
  }

  const session = getSession();
  target.className = "user-menu";
  updateNavigationForSession(session);

  if (session) {
    target.innerHTML = `
      <span class="user-label">${session.name}</span>
      ${session.role !== "ADMIN" ? `<a href="profile.html" class="profile-link-btn" title="Profile">&#128100;</a>` : ""}
      <a href="#" id="logoutLink" class="login-link-btn">Logout</a>
    `;

    document.getElementById("logoutLink").addEventListener("click", (event) => {
      event.preventDefault();
      clearSession();
      goLogin();
    });
  } else {
    target.innerHTML = `
      <a href="user-login.html" class="login-link-btn">Login</a>
    `;
  }
}

function updateNavigationForSession(session) {
  const nav = document.querySelector(".menu-links");
  const brand = document.querySelector(".logo-link");
  if (!nav || !brand) {
    return;
  }

  const primaryLinks = Array.from(nav.querySelectorAll("a")).filter((link) => !link.closest("#headerUserArea"));
  if (session?.role === "ADMIN") {
    brand.setAttribute("href", "admin.html");
    primaryLinks.forEach((link) => {
      link.setAttribute("href", "admin.html");
      link.classList.toggle("active", link.textContent.trim().toLowerCase() === "admin");
      if (link.textContent.trim().toLowerCase() !== "admin") {
        link.style.display = "none";
      }
    });
    return;
  }

  brand.setAttribute("href", "index.html");
  primaryLinks.forEach((link) => {
    link.style.display = "";
  });

  if (session?.role === "DOCTOR") {
    primaryLinks.forEach((link) => {
      if (link.getAttribute("href") === "doctors.html") {
        link.style.display = "none";
      }
    });
  }
}

function validateEmail(value) {
  return EMAIL_PATTERN.test((value || "").trim());
}

function validatePhone(value) {
  return PHONE_PATTERN.test((value || "").trim());
}

function validatePassword(value) {
  const password = value || "";
  return password.length >= 8 && PASSWORD_NUMBER_PATTERN.test(password) && PASSWORD_SPECIAL_PATTERN.test(password);
}

function requireAdminSession() {
  const session = getSession();
  if (!session || session.role !== "ADMIN") {
    window.location.href = "admin-login.html";
    return null;
  }
  return session;
}

function enforceSessionNavigation() {
  const session = getSession();
  const currentPage = window.location.pathname.split("/").pop() || "index.html";
  const adminPages = new Set(["admin.html", "admin-login.html"]);
  const publicPages = new Set(["user-login.html", "register.html", "admin-login.html", "login.html"]);

  if (!session && !publicPages.has(currentPage)) {
    goLogin();
    return;
  }

  if (session && publicPages.has(currentPage) && currentPage !== "admin-login.html") {
    goHome();
    return;
  }

  if (session?.role === "ADMIN" && !adminPages.has(currentPage)) {
    window.location.replace("admin.html");
    return;
  }

  if (currentPage === "admin-login.html" && session?.role === "ADMIN") {
    window.location.replace("admin.html");
  }

  if (session?.role === "DOCTOR" && currentPage === "doctors.html") {
    goHome();
  }
}

document.addEventListener("DOMContentLoaded", () => {
  enforceSessionNavigation();
  updateUserArea();
});
