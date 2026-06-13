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
  rememberToastForNextPage();
  window.location.replace("index.html");
}

function goLogin() {
  rememberToastForNextPage();
  window.location.replace("user-login.html");
}

function showMessage(elementId, message, type) {
  const text = String(message || "").trim();
  const box = document.getElementById(elementId);

  if (box) {
    box.className = "message";
    box.textContent = "";
  }

  if (!text) {
    return;
  }

  showToast(text, type);
}

function showToast(message, type = "success") {
  injectToastStyles();

  const container = getToastContainer();
  const toast = document.createElement("div");
  const kind = type === "error" ? "error" : "success";
  toast.className = `app-toast ${kind}`;

  const title = kind === "error" ? "Action needed" : "Success";
  toast.innerHTML = `
    <div class="toast-marker"></div>
    <div class="toast-body">
      <strong>${escapeText(title)}</strong>
      <span>${escapeText(message)}</span>
    </div>
    <button type="button" class="toast-close" aria-label="Close message">x</button>
  `;

  container.appendChild(toast);
  window.__careBridgeLastToast = { message, type: kind };

  const closeToast = () => {
    toast.classList.add("leaving");
    window.setTimeout(() => toast.remove(), 180);
  };

  toast.querySelector(".toast-close").addEventListener("click", closeToast);
  window.setTimeout(closeToast, kind === "error" ? 4800 : 3400);
}

function rememberToastForNextPage() {
  if (!window.__careBridgeLastToast) {
    return;
  }

  sessionStorage.setItem("careBridgeToast", JSON.stringify(window.__careBridgeLastToast));
  window.__careBridgeLastToast = null;
}

function showRememberedToast() {
  const value = sessionStorage.getItem("careBridgeToast");
  if (!value) {
    return;
  }

  sessionStorage.removeItem("careBridgeToast");

  try {
    const toast = JSON.parse(value);
    if (toast?.message) {
      showToast(toast.message, toast.type);
    }
  } catch (error) {
    sessionStorage.removeItem("careBridgeToast");
  }
}

function getToastContainer() {
  let container = document.getElementById("toastContainer");
  if (!container) {
    container = document.createElement("div");
    container.id = "toastContainer";
    container.setAttribute("aria-live", "polite");
    container.setAttribute("aria-atomic", "true");
    document.body.appendChild(container);
  }
  return container;
}

function injectToastStyles() {
  if (document.getElementById("toastStyles")) {
    return;
  }

  const style = document.createElement("style");
  style.id = "toastStyles";
  style.textContent = `
    #toastContainer {
      position: fixed;
      top: 18px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 9999;
      width: min(420px, calc(100% - 28px));
      display: grid;
      gap: 10px;
      pointer-events: none;
    }

    .app-toast {
      position: relative;
      display: grid;
      grid-template-columns: 5px 1fr auto;
      gap: 14px;
      align-items: start;
      padding: 14px 14px 14px 0;
      overflow: hidden;
      border: 1px solid #d7e0e7;
      border-radius: 10px;
      background: #ffffff;
      box-shadow: 0 14px 34px rgba(15, 64, 88, 0.16);
      color: #1c2430;
      pointer-events: auto;
      animation: toastIn 0.22s ease-out;
    }

    .app-toast.leaving {
      animation: toastOut 0.18s ease-in forwards;
    }

    .toast-marker {
      width: 5px;
      height: 100%;
      min-height: 48px;
      border-radius: 0 999px 999px 0;
      background: #138a5b;
    }

    .app-toast.error .toast-marker {
      background: #b42318;
    }

    .toast-body {
      display: grid;
      gap: 3px;
      line-height: 1.35;
    }

    .toast-body strong {
      font-size: 0.92rem;
      color: #0f4058;
    }

    .app-toast.error .toast-body strong {
      color: #8f1d14;
    }

    .toast-body span {
      font-size: 0.95rem;
      color: #435466;
    }

    .toast-close {
      width: 28px;
      height: 28px;
      border: 0;
      border-radius: 8px;
      cursor: pointer;
      color: #5d6b7a;
      background: #f4f7fa;
      font: inherit;
      font-weight: 800;
      line-height: 1;
    }

    .toast-close:hover {
      color: #0f4058;
      background: #eef7fa;
    }

    @keyframes toastIn {
      from { opacity: 0; transform: translateY(-12px); }
      to { opacity: 1; transform: translateY(0); }
    }

    @keyframes toastOut {
      to { opacity: 0; transform: translateY(-10px); }
    }
  `;
  document.head.appendChild(style);
}

function ensureAppFooter() {
  const wrapper = document.querySelector(".page-wrapper");
  if (!wrapper) {
    return;
  }

  let footer = wrapper.querySelector(".page-footer");
  if (!footer) {
    footer = document.createElement("footer");
    footer.className = "page-footer";
    wrapper.appendChild(footer);
  }

  if (!footer.querySelector(".footer-shell")) {
    footer.innerHTML = `
    <div class="footer-shell">
      <div class="footer-grid">
        <section class="footer-brand-panel">
          <a class="footer-logo" href="index.html" aria-label="CareBridge home">
            <span class="footer-logo-icon">+</span>
            <strong>CareBridge</strong>
          </a>
          <p>Simple appointment management for patients, doctors, booking support, and healthcare updates.</p>
          <div class="footer-note">Verified doctors with reliable patient support</div>
        </section>

        <section>
          <h3>Services</h3>
          <nav class="footer-link-list" aria-label="Footer services">
            <a href="doctors.html">Find doctors</a>
            <a href="book.html">Book appointment</a>
            <a href="profile.html">Patient profile</a>
            <a href="user-login.html">Patient login</a>
            <a href="user-login.html">Doctor login</a>
          </nav>
        </section>

        <section>
          <h3>Contact</h3>
          <div class="footer-contact-list">
            <span>+91 9158852129</span>
            <span>kirantalekar444@gmail.com</span>
            <span>Baramati, Maharashtra</span>
            <span>Mon - Sun, 8:00 AM - 9:00 PM</span>
          </div>
        </section>

        <section>
          <h3>For Patients</h3>
          <p>Book doctor appointments, manage visit updates, and get help when you need support.</p>
          <div class="footer-help">
            <strong>Need help?</strong>
            <span>Contact us for booking, login, or profile issues.</span>
          </div>
        </section>
      </div>

      <div class="footer-bottom">
        <span>© 2026 CareBridge. All Rights Reserved.</span>
        <nav class="footer-bottom-links" aria-label="Footer quick links">
          <a href="contact.html">Contact</a>
          <a href="index.html">Privacy Policy</a>
          <a href="index.html">Terms</a>
          <span>Serving patients across Baramati and nearby cities.</span>
        </nav>
      </div>
    </div>
  `;
  }

  injectFooterStyles();
}

function injectFooterStyles() {
  if (document.getElementById("footerStyles")) {
    return;
  }

  const style = document.createElement("style");
  style.id = "footerStyles";
  style.textContent = `
    .page-wrapper {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }

    .page-wrapper > main {
      flex: 1;
    }

    .page-footer {
      margin-top: auto;
      background: #121d26;
      color: #f5f8fb;
    }

    .footer-shell {
      width: min(1180px, calc(100% - 32px));
      margin: 0 auto;
      padding: 42px 0 26px;
    }

    .footer-grid {
      display: grid;
      grid-template-columns: 1.2fr 1fr 1fr 1.1fr;
      gap: 42px;
    }

    .footer-brand-panel,
    .footer-grid section {
      display: grid;
      align-content: start;
      gap: 16px;
    }

    .footer-logo {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      color: #ffffff;
      font-size: 1.55rem;
      font-weight: 800;
    }

    .footer-logo-icon {
      width: 34px;
      height: 34px;
      display: grid;
      place-items: center;
      border-radius: 8px;
      color: #121d26;
      background: #ffffff;
    }

    .footer-grid h3 {
      margin: 0;
      color: #ffffff;
      font-size: 1.15rem;
    }

    .footer-grid p,
    .footer-note,
    .footer-contact-list span,
    .footer-link-list a,
    .footer-help span,
    .footer-bottom,
    .footer-bottom a {
      color: #d9e4eb;
      line-height: 1.65;
      font-size: 0.96rem;
    }

    .footer-grid p {
      margin: 0;
    }

    .footer-note,
    .footer-help {
      padding-left: 14px;
      border-left: 3px solid #ffd02f;
      font-weight: 700;
    }

    .footer-help {
      display: grid;
      gap: 4px;
    }

    .footer-help strong {
      color: #ffffff;
    }

    .footer-link-list,
    .footer-contact-list {
      display: grid;
      gap: 12px;
    }

    .footer-link-list a {
      width: fit-content;
      font-weight: 600;
    }

    .footer-link-list a:hover,
    .footer-bottom a:hover {
      color: #ffffff;
    }

    .footer-bottom {
      display: flex;
      justify-content: space-between;
      gap: 14px;
      flex-wrap: wrap;
      margin-top: 34px;
      padding-top: 20px;
      border-top: 1px solid rgba(255, 255, 255, 0.14);
    }

    .footer-bottom-links {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
      align-items: center;
    }

    .footer-bottom a {
      font-weight: 700;
    }

    @media (max-width: 980px) {
      .footer-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    @media (max-width: 640px) {
      .footer-shell {
        width: min(100% - 24px, 1180px);
        padding: 34px 0 22px;
      }

      .footer-grid {
        grid-template-columns: 1fr;
        gap: 28px;
      }

      .footer-bottom {
        flex-direction: column;
      }
    }
  `;
  document.head.appendChild(style);
}

function escapeText(value) {
  const span = document.createElement("span");
  span.textContent = value == null ? "" : String(value);
  return span.innerHTML;
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
      ${session.role === "PATIENT" ? `<a href="profile.html" class="profile-link-btn" title="Profile">&#128100;</a>` : ""}
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
  const authPages = new Set(["user-login.html", "register.html", "login.html"]);
  const publicPages = new Set([...authPages, "admin-login.html", "contact.html"]);

  if (!session && !publicPages.has(currentPage)) {
    goLogin();
    return;
  }

  if (session && authPages.has(currentPage)) {
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
  ensureAppFooter();
  showRememberedToast();
});
