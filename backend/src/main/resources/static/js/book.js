let selectedPatientAppointmentId = null;

async function loadDoctors(selectedDoctorId) {
  const doctors = await request("/doctors");
  const doctorSelect = document.getElementById("doctorSelect");

  doctorSelect.innerHTML = doctors.map((doctor) => `
    <option value="${doctor.id}" ${String(doctor.id) === String(selectedDoctorId) ? "selected" : ""}>
      ${doctor.name} - ${doctor.specialization}
    </option>
  `).join("");
}

async function loadAppointments() {
  const session = getSession();
  const list = document.getElementById("myAppointmentList");
  const emptyTemplate = document.getElementById("appointmentEmptyMessageTemplate");

  if (!session || (session.role !== "PATIENT" && session.role !== "DOCTOR")) {
    const emptyState = emptyTemplate.content.firstElementChild.cloneNode(true);
    emptyState.querySelector('[data-field="message"]').textContent = "Login to see appointments.";
    list.replaceChildren(emptyState);
    return;
  }

  try {
    if (session.role === "DOCTOR") {
      await loadDoctorAppointments(session, list, emptyTemplate);
      return;
    }

    const cardTemplate = document.getElementById("myAppointmentCardTemplate");
    const appointments = await request(`/appointments/patient/${session.id}`);
    if (!appointments.length) {
      const emptyState = emptyTemplate.content.firstElementChild.cloneNode(true);
      emptyState.querySelector('[data-field="message"]').textContent = "No appointments booked yet.";
      list.replaceChildren(emptyState);
      return;
    }

    list.innerHTML = "";
    appointments.forEach((appointment) => {
      const card = cardTemplate.content.firstElementChild.cloneNode(true);
      card.querySelector('[data-field="doctorName"]').textContent = appointment.doctorName;
      card.querySelector('[data-field="specialization"]').textContent = appointment.specialization;
      card.querySelector('[data-field="date"]').textContent = appointment.appointmentDate;
      card.querySelector('[data-field="time"]').textContent = appointment.timeSlot;
      card.querySelector('[data-field="status"]').textContent = appointment.status;
      card.querySelector('[data-field="symptoms"]').textContent = appointment.symptoms || "Not provided";
      card.querySelector('[data-field="rescheduleButton"]').addEventListener("click", () => openReschedulePopup(appointment));
      card.querySelector('[data-field="cancelButton"]').addEventListener("click", async () => {
        await cancelPatientAppointment(session.id, appointment.id);
      });
      list.appendChild(card);
    });
  } catch (error) {
    const emptyState = emptyTemplate.content.firstElementChild.cloneNode(true);
    emptyState.querySelector('[data-field="message"]').textContent = error.message;
    list.replaceChildren(emptyState);
  }
}

async function loadDoctorAppointments(session, list, emptyTemplate) {
  const cardTemplate = document.getElementById("doctorAppointmentCardTemplate");
  const appointments = await request(`/appointments/doctor/${session.doctorProfileId}`);

  if (!appointments.length) {
    const emptyState = emptyTemplate.content.firstElementChild.cloneNode(true);
    emptyState.querySelector('[data-field="message"]').textContent = "No appointments received yet.";
    list.replaceChildren(emptyState);
    return;
  }

  list.innerHTML = "";
  appointments.forEach((appointment) => {
    const card = cardTemplate.content.firstElementChild.cloneNode(true);
    card.querySelector('[data-field="patientName"]').textContent = appointment.patientName;
    card.querySelector('[data-field="doctorName"]').textContent = appointment.doctorName;
    card.querySelector('[data-field="date"]').textContent = appointment.appointmentDate;
    card.querySelector('[data-field="time"]').textContent = appointment.timeSlot;
    card.querySelector('[data-field="status"]').textContent = appointment.status;
    card.querySelector('[data-field="symptoms"]').textContent = appointment.symptoms || "Not provided";
    const statusSelect = card.querySelector('[data-field="statusSelect"]');
    statusSelect.value = appointment.status;
    card.querySelector('[data-field="updateButton"]').addEventListener("click", async () => {
      await updateDoctorAppointmentStatus(session.doctorProfileId, appointment.id, statusSelect.value);
    });
    list.appendChild(card);
  });
}

async function updateDoctorAppointmentStatus(doctorId, appointmentId, status) {
  try {
    await request(`/appointments/doctor/${doctorId}/${appointmentId}/status`, {
      method: "PUT",
      body: JSON.stringify({ status })
    });

    showMessage("bookingMessage", "Appointment status updated and email sent to patient.", "success");
    await loadAppointments();
  } catch (error) {
    showMessage("bookingMessage", error.message, "error");
  }
}

async function cancelPatientAppointment(patientId, appointmentId) {
  try {
    await request(`/appointments/patient/${patientId}/${appointmentId}/cancel`, {
      method: "PUT"
    });

    showMessage("bookingMessage", "Appointment cancelled successfully.", "success");
    await loadAppointments();
  } catch (error) {
    showMessage("bookingMessage", error.message, "error");
  }
}

async function saveRescheduledAppointment(event) {
  event.preventDefault();

  const session = getSession();
  if (!session || session.role !== "PATIENT" || !selectedPatientAppointmentId) {
    return;
  }

  try {
    await request(`/appointments/patient/${session.id}/${selectedPatientAppointmentId}/reschedule`, {
      method: "PUT",
      body: JSON.stringify({
        appointmentDate: document.getElementById("rescheduleDate").value,
        timeSlot: document.getElementById("rescheduleTime").value.trim(),
        symptoms: document.getElementById("rescheduleSymptoms").value.trim()
      })
    });

    closeReschedulePopup();
    showMessage("bookingMessage", "Appointment rescheduled successfully.", "success");
    await loadAppointments();
  } catch (error) {
    showMessage("bookingMessage", error.message, "error");
  }
}

async function book(event) {
  event.preventDefault();

  const session = getSession();
  if (!session || session.role !== "PATIENT") {
    showMessage("bookingMessage", "Please login as a patient before booking an appointment.", "error");
    return;
  }

  const payload = {
    doctorId: Number(document.getElementById("doctorSelect").value),
    patientId: session.id,
    appointmentDate: document.getElementById("visitDate").value,
    timeSlot: document.getElementById("visitTime").value.trim(),
    symptoms: document.getElementById("visitReason").value.trim()
  };

  try {
    await request("/appointments/book", {
      method: "POST",
      body: JSON.stringify(payload)
    });

    showMessage("bookingMessage", "Appointment Booked", "success");
    document.getElementById("appointmentForm").reset();
    await loadAppointments();
  } catch (error) {
    showMessage("bookingMessage", error.message, "error");
  }
}

document.addEventListener("DOMContentLoaded", async () => {
  const query = new URLSearchParams(window.location.search);
  const doctorId = query.get("doctorId");
  const session = getSession();

  setupBookingPage(session);

  if (session?.role !== "DOCTOR") {
    try {
      await loadDoctors(doctorId);
    } catch (error) {
      showMessage("bookingMessage", error.message, "error");
    }
  }

  document.getElementById("appointmentForm").addEventListener("submit", book);
  document.getElementById("rescheduleForm").addEventListener("submit", saveRescheduledAppointment);
  document.getElementById("closeRescheduleButton").addEventListener("click", closeReschedulePopup);
  document.getElementById("cancelRescheduleButton").addEventListener("click", closeReschedulePopup);
  loadAppointments();
});

function setupBookingPage(session) {
  const appointmentForm = document.getElementById("appointmentForm");
  const bookingLeftCard = document.getElementById("bookingLeftCard");
  const bookingMainTitle = document.getElementById("bookingMainTitle");
  const bookingMainText = document.getElementById("bookingMainText");
  const appointmentListTitle = document.getElementById("appointmentListTitle");
  const appointmentListText = document.getElementById("appointmentListText");

  if (session?.role === "DOCTOR") {
    bookingLeftCard.style.display = "none";
    bookingLeftCard.hidden = true;
    appointmentListTitle.textContent = "Received appointments";
    appointmentListText.textContent = "Every booked appointment will appear here for doctor review.";
    return;
  }

  bookingLeftCard.style.display = "";
  bookingLeftCard.hidden = false;
  bookingMainTitle.textContent = "Book appointment";
  bookingMainText.textContent = "Choose a doctor, select a date, and confirm the appointment.";
  appointmentListTitle.textContent = "Your appointments";
  appointmentListText.textContent = "After booking, your upcoming appointments will show here.";
  appointmentForm.hidden = false;
}

function openReschedulePopup(appointment) {
  selectedPatientAppointmentId = appointment.id;
  document.getElementById("rescheduleAppointmentId").value = appointment.id;
  document.getElementById("rescheduleDate").value = appointment.appointmentDate;
  document.getElementById("rescheduleTime").value = appointment.timeSlot;
  document.getElementById("rescheduleSymptoms").value = appointment.symptoms || "";
  document.getElementById("reschedulePopup").classList.add("show");
}

function closeReschedulePopup() {
  selectedPatientAppointmentId = null;
  document.getElementById("reschedulePopup").classList.remove("show");
}
