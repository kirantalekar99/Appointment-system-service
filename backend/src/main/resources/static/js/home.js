document.addEventListener("DOMContentLoaded", async () => {
  updateUserArea();
  const session = getSession();
  const featuredDoctors = document.getElementById("todayDoctorList");
  const doctorTemplate = document.getElementById("todayDoctorCardTemplate");
  const emptyTemplate = document.getElementById("todayDoctorEmptyTemplate");
  const availableDoctorsSection = document.getElementById("availableDoctorsSection");
  const today = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"][new Date().getDay()];

  setupHomeContent(session);

  if (session?.role === "DOCTOR") {
    availableDoctorsSection.style.display = "none";
  }

  try {
    const doctors = await request("/doctors");
    document.getElementById("allDoctorCount").textContent = doctors.length;

    const specialties = new Set(doctors.map((doctor) => doctor.specialization));
    document.getElementById("specialtyCountDisplay").textContent = specialties.size;

    const availableToday = doctors.filter((doctor) =>
      (doctor.availableDays || "")
        .split(",")
        .map((day) => day.trim())
        .includes(today)
    );

    if (session?.role === "DOCTOR") {
      return;
    }

    if (!availableToday.length) {
      featuredDoctors.replaceChildren(emptyTemplate.content.firstElementChild.cloneNode(true));
      return;
    }

    featuredDoctors.innerHTML = "";
    availableToday.forEach((doctor) => {
      const card = doctorTemplate.content.firstElementChild.cloneNode(true);
      card.querySelector('[data-field="name"]').textContent = doctor.name;
      card.querySelector('[data-field="specialization"]').textContent = doctor.specialization;
      card.querySelector('[data-field="link"]').href = `book.html?doctorId=${doctor.id}`;
      featuredDoctors.appendChild(card);
    });
  } catch (error) {
    if (session?.role !== "DOCTOR") {
      featuredDoctors.replaceChildren(emptyTemplate.content.firstElementChild.cloneNode(true));
    }
  }
});

function setupHomeContent(session) {
  const heroTag = document.getElementById("homeHeroTag");
  const heroTitle = document.getElementById("homeHeroTitle");
  const heroText = document.getElementById("homeHeroText");
  const primaryHeroButton = document.getElementById("primaryHeroButton");
  const secondaryHeroButton = document.getElementById("secondaryHeroButton");
  const thirdStatLabel = document.getElementById("accessStatLabel");

  document.body.dataset.sessionRole = session?.role || "GUEST";
  secondaryHeroButton.hidden = false;

  if (!session) {
    return;
  }

  if (session.role === "DOCTOR") {
    heroTag.textContent = "Doctor workspace";
    heroTitle.textContent = "Review appointments and update visit status.";
    heroText.textContent = "See your patient bookings in one place, update appointment progress, and keep your schedule clear without extra screens.";
    primaryHeroButton.textContent = "View Appointments";
    primaryHeroButton.href = "book.html";
    secondaryHeroButton.hidden = true;
    if (thirdStatLabel) {
      thirdStatLabel.textContent = "Appointment management";
    }
    return;
  }

  heroTag.textContent = "Patient workspace";
  heroTitle.textContent = "Book appointments without confusion.";
  heroText.textContent = "Choose a doctor, reserve a time slot, and manage your upcoming appointments from a simple patient dashboard.";
  primaryHeroButton.textContent = "Book Appointment";
  primaryHeroButton.href = "book.html";
  secondaryHeroButton.textContent = "View All Doctors";
  secondaryHeroButton.href = "doctors.html";
  if (thirdStatLabel) {
    thirdStatLabel.textContent = "Easy online access for patients";
  }
}
