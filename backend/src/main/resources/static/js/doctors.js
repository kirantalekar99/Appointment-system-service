document.addEventListener("DOMContentLoaded", async () => {
  const session = getSession();
  if (session?.role === "DOCTOR") {
    goHome();
    return;
  }

  const list = document.getElementById("allDoctorList");
  const cardTemplate = document.getElementById("allDoctorCardTemplate");
  const emptyTemplate = document.getElementById("allDoctorEmptyTemplate");
  const errorTemplate = document.getElementById("allDoctorErrorTemplate");

  try {
    const doctors = await request("/doctors");

    if (!doctors.length) {
      list.replaceChildren(emptyTemplate.content.firstElementChild.cloneNode(true));
      return;
    }

    list.innerHTML = "";
    doctors.forEach((doctor) => {
      const card = cardTemplate.content.firstElementChild.cloneNode(true);
      card.querySelector('[data-field="name"]').textContent = doctor.name;
      card.querySelector('[data-field="specialization"]').textContent = doctor.specialization;
      card.querySelector('[data-field="experience"]').textContent = `${doctor.experience}+ years experience`;
      card.querySelector('[data-field="days"]').textContent = doctor.availableDays;
      card.querySelector('[data-field="time"]').textContent = doctor.availableTime;
      card.querySelector('[data-field="bio"]').textContent = doctor.bio;
      card.querySelector('[data-field="link"]').href = `book.html?doctorId=${doctor.id}`;
      list.appendChild(card);
    });
  } catch (error) {
    const errorNode = errorTemplate.content.firstElementChild.cloneNode(true);
    errorNode.querySelector('[data-field="message"]').textContent = error.message;
    list.replaceChildren(errorNode);
  }
});
