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

  if (!session) {
    return;
  }

  if (session.role === "DOCTOR") {
    heroTag.textContent = "Simple healthcare access for every doctor";
    heroTitle.textContent = "Manage appointments and patient updates with ease.";
    heroText.textContent = "This appointment system helps doctors review schedules, track patient bookings, and manage visit status without confusion. The interface is simple, readable, and designed to keep important health information easy to understand.";
    primaryHeroButton.textContent = "Open Booking";
    primaryHeroButton.href = "book.html";
    secondaryHeroButton.textContent = "View Profile";
    secondaryHeroButton.href = "profile.html";
    return;
  }

  primaryHeroButton.textContent = "Book Appointment";
  primaryHeroButton.href = "book.html";
  secondaryHeroButton.textContent = "View All Doctors";
  secondaryHeroButton.href = "doctors.html";
}
