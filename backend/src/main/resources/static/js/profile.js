async function loadProfile() {
  const session = getSession();
  if (!session) {
    goLogin();
    return;
  }

  const profile = await request(`/auth/profile/${session.id}`);
  document.getElementById("profileName").value = profile.name || "";
  document.getElementById("profileEmail").value = profile.email || "";
  document.getElementById("profilePhone").value = profile.phone || "";

  const doctorFields = document.getElementById("doctorProfileFields");
  if (profile.role === "DOCTOR") {
    document.getElementById("profileTitle").textContent = "Doctor profile";
    document.getElementById("profileSubtitle").textContent = "Update your doctor details, timings, and personal information.";
    doctorFields.hidden = false;
    document.getElementById("profileSpecialization").value = profile.specialization || "";
    document.getElementById("profileExperience").value = profile.experience || "";
    document.getElementById("profileBio").value = profile.bio || "";
    document.getElementById("profileAvailableDays").value = profile.availableDays || "";
    document.getElementById("profileAvailableTime").value = profile.availableTime || "";
  } else {
    document.getElementById("profileTitle").textContent = "Patient profile";
    document.getElementById("profileSubtitle").textContent = "Update your patient account information here.";
    doctorFields.hidden = true;
  }
}

async function saveProfile(event) {
  event.preventDefault();
  const session = getSession();
  if (!session) {
    goLogin();
    return;
  }

  const payload = {
    name: document.getElementById("profileName").value.trim(),
    email: document.getElementById("profileEmail").value.trim(),
    phone: document.getElementById("profilePhone").value.trim(),
    password: document.getElementById("profilePassword").value,
    specialization: document.getElementById("profileSpecialization")?.value.trim(),
    experience: document.getElementById("profileExperience")?.value ? Number(document.getElementById("profileExperience").value) : null,
    bio: document.getElementById("profileBio")?.value.trim(),
    availableDays: document.getElementById("profileAvailableDays")?.value.trim(),
    availableTime: document.getElementById("profileAvailableTime")?.value.trim()
  };

  try {
    const response = await request(`/auth/profile/${session.id}`, {
      method: "PUT",
      body: JSON.stringify(payload)
    });

    saveSession(response);
    showMessage("profileMessage", "Profile updated successfully.", "success");
    document.getElementById("profilePassword").value = "";
  } catch (error) {
    showMessage("profileMessage", error.message, "error");
  }
}

document.addEventListener("DOMContentLoaded", async () => {
  await loadProfile();
  document.getElementById("profileForm").addEventListener("submit", saveProfile);
});
