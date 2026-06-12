let editingDoctorId = null;
let adminDoctorsCache = [];

function showPopup(message) {
  window.alert(message);
}

function setFieldText(root, selector, value) {
  const element = root.querySelector(selector);
  if (element) {
    element.textContent = value ?? "";
  }
}

function createEmptyState(message) {
  const emptyState = document.getElementById("emptyMessageTemplate").content.firstElementChild.cloneNode(true);
  setFieldText(emptyState, '[data-field="message"]', message);
  return emptyState;
}

function doctorFormPayload() {
  return {
    name: document.getElementById("doctorFullName").value.trim(),
    email: document.getElementById("doctorEmailAddress").value.trim(),
    phone: document.getElementById("doctorPhoneNumber").value.trim(),
    specialization: document.getElementById("doctorSpecialty").value.trim(),
    experience: Number(document.getElementById("doctorYearsExperience").value),
    availableDays: document.getElementById("doctorWorkDays").value.trim(),
    availableTime: document.getElementById("doctorWorkTime").value.trim()
  };
}

function validateDoctorPayload(payload, isEdit) {
  if (!payload.name || !payload.specialization || !payload.availableDays || !payload.availableTime) {
    return "Please fill in all doctor fields.";
  }

  if (!validateEmail(payload.email)) {
    return "Please enter a valid doctor email address.";
  }

  if (!validatePhone(payload.phone)) {
    return "Doctor phone number must 10 digits.";
  }

  if (!payload.experience || payload.experience < 1) {
    return "Doctor experience must be at least 1 year.";
  }

  return null;
}

function resetDoctorForm() {
  editingDoctorId = null;
  document.getElementById("doctorDetailsForm").reset();
  document.getElementById("saveDoctorButton").textContent = "Add Doctor";
  document.getElementById("cancelDoctorButton").textContent = "Cancel";
  document.getElementById("doctorFormTitle").textContent = "Add New Doctor";
  document.getElementById("doctorFormText").textContent = "Fill the form to create a new doctor profile.";
}

function openDoctorModal(mode) {
  document.getElementById("doctorFormPopup").classList.add("show");
  if (mode === "ADD") {
    resetDoctorForm();
  }
}

function closeDoctorModal() {
  document.getElementById("doctorFormPopup").classList.remove("show");
  resetDoctorForm();
}

function openAdminPasswordPopup() {
  document.getElementById("adminPasswordPopup").classList.add("show");
}

function closeAdminPasswordPopup() {
  document.getElementById("adminPasswordPopup").classList.remove("show");
  document.getElementById("adminLoginPassword").value = "";
}

async function loadAdminAccount() {
  const admin = await request("/admin/account");
  document.getElementById("currentAdminEmail").textContent = admin.email;
  document.getElementById("adminLoginEmail").value = admin.email;
}

async function loadSummary() {
  const summary = await request("/admin/summary");
  document.getElementById("doctorTotalCount").textContent = summary.totalDoctors;
  document.getElementById("patientTotalCount").textContent = summary.totalPatients;
  document.getElementById("appointmentTotalCount").textContent = summary.totalAppointments;
  document.getElementById("bookedCount").textContent = summary.bookedAppointments;
  document.getElementById("completedCount").textContent = summary.completedAppointments;
  document.getElementById("cancelledCount").textContent = summary.cancelledAppointments;
}

async function loadDoctors() {
  const doctors = await request("/admin/doctors");
  adminDoctorsCache = doctors;
  const target = document.getElementById("doctorManageList");
  const cardTemplate = document.getElementById("doctorManageCardTemplate");

  if (!doctors.length) {
    target.replaceChildren(createEmptyState("No doctors added yet."));
    return;
  }

  target.innerHTML = "";
  doctors.forEach((doctor) => {
    const card = cardTemplate.content.firstElementChild.cloneNode(true);
    card.dataset.doctorId = String(doctor.id);
    setFieldText(card, '[data-field="name"]', doctor.name);
    setFieldText(card, '[data-field="specialization"]', doctor.specialization);
    setFieldText(card, '[data-field="experience"]', `${doctor.experience}+ years`);
    setFieldText(card, '[data-field="days"]', doctor.availableDays);
    setFieldText(card, '[data-field="time"]', doctor.availableTime);
    setFieldText(card, '[data-field="bio"]', doctor.bio);
    setFieldText(card, '[data-field="email"]', doctor.email);
    target.appendChild(card);
  });
}

function fillDoctorForm(doctor) {
  editingDoctorId = doctor.id;
  document.getElementById("doctorFullName").value = doctor.name;
  document.getElementById("doctorEmailAddress").value = doctor.email;
  document.getElementById("doctorPhoneNumber").value = doctor.phone || "";
  document.getElementById("doctorSpecialty").value = doctor.specialization;
  document.getElementById("doctorYearsExperience").value = doctor.experience;
  document.getElementById("doctorWorkDays").value = doctor.availableDays;
  document.getElementById("doctorWorkTime").value = doctor.availableTime;
  document.getElementById("doctorFormTitle").textContent = "Edit Doctor";
  document.getElementById("doctorFormText").textContent = "Update the selected doctor profile details.";
  document.getElementById("saveDoctorButton").textContent = "Update Doctor";
  showMessage("doctorStatusMessage", "Editing doctor details.", "success");
  openDoctorModal("EDIT");
}

async function handleDoctorListClick(event) {
  const actionButton = event.target.closest("[data-action]");
  if (!actionButton) {
    return;
  }

  const card = actionButton.closest("[data-doctor-id]");
  if (!card) {
    return;
  }

  const doctorId = Number(card.dataset.doctorId);
  const doctor = adminDoctorsCache.find((item) => item.id === doctorId);
  if (!doctor) {
    showMessage("doctorStatusMessage", "Doctor details could not be loaded. Please refresh and try again.", "error");
    return;
  }

  const action = actionButton.dataset.action;
  if (action === "edit") {
    fillDoctorForm(doctor);
    return;
  }

  if (action === "delete") {
    if (!window.confirm(`Delete ${doctor.name}?`)) {
      return;
    }

    try {
      await request(`/admin/doctors/${doctor.id}`, { method: "DELETE" });
      showMessage("doctorStatusMessage", "Doctor Information Deleted", "success");
      showPopup("Doctor Deleted");
      await refreshAdminData();
    } catch (error) {
      showMessage("doctorStatusMessage", error.message, "error");
    }
  }
}

async function loadAppointments() {
  const appointments = await request("/admin/appointments");
  const target = document.getElementById("appointmentManageList");
  const cardTemplate = document.getElementById("appointmentManageCardTemplate");

  if (!appointments.length) {
    target.replaceChildren(createEmptyState("No appointments are available yet."));
    return;
  }

  target.innerHTML = "";
  appointments.forEach((appointment) => {
    const card = cardTemplate.content.firstElementChild.cloneNode(true);
    const statusSelect = card.querySelector('[data-field="statusSelect"]');
    setFieldText(card, '[data-field="doctorName"]', appointment.doctorName);
    setFieldText(card, '[data-field="patientLine"]',
      `Patient: ${appointment.patientName} (${appointment.patientEmail || "No email"})`);
    setFieldText(card, '[data-field="date"]', appointment.appointmentDate);
    setFieldText(card, '[data-field="time"]', appointment.timeSlot);
    setFieldText(card, '[data-field="status"]', appointment.status);
    setFieldText(card, '[data-field="symptoms"]', appointment.symptoms || "No symptoms added.");
    if (statusSelect) {
      statusSelect.value = appointment.status;
    }

    card.querySelector('[data-action="save-status"]').addEventListener("click", async () => {
      const status = statusSelect.value;
      try {
        await request(`/admin/appointments/${appointment.id}/status`, {
          method: "PATCH",
          body: JSON.stringify({ status })
        });
        showMessage("appointmentStatusMessage", "Appointment status updated.", "success");
        showPopup("Appointment status updated");
        await refreshAdminData();
      } catch (error) {
        showMessage("appointmentStatusMessage", error.message, "error");
      }
    });

    card.querySelector('[data-action="cancel"]').addEventListener("click", async () => {
      try {
        await request(`/admin/appointments/${appointment.id}/cancel`, { method: "PATCH" });
        showMessage("appointmentStatusMessage", "Appointment cancelled.", "success");
        showPopup("Appointment Cancelled");
        await refreshAdminData();
      } catch (error) {
        showMessage("appointmentStatusMessage", error.message, "error");
      }
    });
    target.appendChild(card);
  });
}

async function loadNotifications() {
  const logs = await request("/admin/notifications");
  const target = document.getElementById("notificationHistoryList");
  const cardTemplate = document.getElementById("notificationHistoryCardTemplate");

  if (!logs.length) {
    target.replaceChildren(createEmptyState("Notification"));
    return;
  }

  target.innerHTML = "";
  logs.forEach((log) => {
    const card = cardTemplate.content.firstElementChild.cloneNode(true);
    card.dataset.notificationId = String(log.id);
    setFieldText(card, '[data-field="channel"]', log.channel);
    setFieldText(card, '[data-field="recipient"]', log.recipient);
    setFieldText(card, '[data-field="message"]', log.message);
    setFieldText(card, '[data-field="createdAt"]', new Date(log.createdAt).toLocaleString());
    target.appendChild(card);
  });
}

async function handleNotificationHistoryClick(event) {
  const actionButton = event.target.closest("[data-action]");
  if (!actionButton || actionButton.dataset.action !== "delete-notification") {
    return;
  }

  const card = actionButton.closest("[data-notification-id]");
  if (!card) {
    return;
  }

  const notificationId = Number(card.dataset.notificationId);
  if (!window.confirm("Delete Notification")) {
    return;
  }

  try {
    await request(`/admin/notifications/${notificationId}`, { method: "DELETE" });
    showMessage("historyStatusMessage", "Notification Deleted", "success");
    showPopup("Notification Deleted");
    await loadNotifications();
  } catch (error) {
    showMessage("historyStatusMessage", error.message, "error");
  }
}

async function refreshAdminData() {
  await Promise.all([loadSummary(), loadAdminAccount(), loadDoctors(), loadAppointments(), loadNotifications()]);
}

async function submitDoctorForm(event) {
  event.preventDefault();
  const payload = doctorFormPayload();
  const message = validateDoctorPayload(payload, Boolean(editingDoctorId));
  if (message) {
    showMessage("doctorStatusMessage", message, "error");
    return;
  }

  try {
    if (editingDoctorId) {
      await request(`/admin/doctors/${editingDoctorId}`, {
        method: "PUT",
        body: JSON.stringify(payload)
      });
      showMessage("doctorStatusMessage", "Doctor Information Updated", "success");
      showPopup("Doctor Information Updated");
    } else {
      await request("/admin/doctors", {
        method: "POST",
        body: JSON.stringify(payload)
      });
      showMessage("doctorStatusMessage", "Doctor Information Added", "success");
      showPopup("Doctor Information Added");
    }

    resetDoctorForm();
    closeDoctorModal();
    await refreshAdminData();
  } catch (error) {
    showMessage("doctorStatusMessage", error.message, "error");
  }
}

async function submitNotification(event) {
  event.preventDefault();
  const payload = {
    channel: document.getElementById("notificationType").value,
    recipientGroup: document.getElementById("notificationAudience").value,
    recipient: document.getElementById("notificationTarget").value.trim(),
    message: document.getElementById("notificationText").value.trim()
  };

  if (!payload.message) {
    showMessage("notificationStatusMessage", "Please enter message", "error");
    return;
  }

  try {
    await request("/admin/notifications", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    document.getElementById("notificationSendForm").reset();
    showMessage("notificationStatusMessage", "Notification Updated", "success");
    showPopup("Notification Sent");
    await loadNotifications();
  } catch (error) {
    showMessage("notificationStatusMessage", error.message, "error");
  }
}

async function submitAdminAccountForm(event) {
  event.preventDefault();
  const email = document.getElementById("adminLoginEmail").value.trim();
  const password = document.getElementById("adminLoginPassword").value;

  if (!validateEmail(email)) {
    showMessage("adminAccountMessage", "Please enter admin email address.", "error");
    return;
  }

  if (password && !validatePassword(password)) {
    showMessage("adminAccountMessage", "Password must be at least 8 characters and include 1 special character.", "error");
    return;
  }

  try {
    const updatedAdmin = await request("/admin/account", {
      method: "PUT",
      body: JSON.stringify({ email, password })
    });

    const currentSession = getSession();
    if (currentSession) {
      currentSession.email = updatedAdmin.email;
      saveSession(currentSession);
    }

    document.getElementById("currentAdminEmail").textContent = updatedAdmin.email;
    document.getElementById("adminLoginPassword").value = "";
    showMessage("adminAccountMessage", "Admin login details updated", "success");
    showPopup("Admin login detail updated");
    closeAdminPasswordPopup();
  } catch (error) {
    showMessage("adminAccountMessage", error.message, "error");
  }
}

document.addEventListener("DOMContentLoaded", async () => {
  const admin = requireAdminSession();
  if (!admin) {
    return;
  }

  updateUserArea();

  document.getElementById("showAdminPasswordPopupButton").addEventListener("click", openAdminPasswordPopup);
  document.getElementById("closeAdminPasswordPopupButton").addEventListener("click", closeAdminPasswordPopup);
  document.getElementById("cancelAdminPasswordButton").addEventListener("click", closeAdminPasswordPopup);
  document.getElementById("showAddDoctorButton").addEventListener("click", () => openDoctorModal("ADD"));
  document.getElementById("closeDoctorPopupButton").addEventListener("click", closeDoctorModal);
  document.getElementById("doctorManageList").addEventListener("click", handleDoctorListClick);
  document.getElementById("doctorDetailsForm").addEventListener("submit", submitDoctorForm);
  document.getElementById("cancelDoctorButton").addEventListener("click", closeDoctorModal);
  document.getElementById("notificationSendForm").addEventListener("submit", submitNotification);
  document.getElementById("notificationHistoryList").addEventListener("click", handleNotificationHistoryClick);
  document.getElementById("adminAccountForm").addEventListener("submit", submitAdminAccountForm);
  resetDoctorForm();

  try {
    await refreshAdminData();
  } catch (error) {
    showMessage("adminStatusMessage", error.message, "error");
  }
});
