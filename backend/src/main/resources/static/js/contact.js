document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("contactForm");
  const nameInput = document.getElementById("contactName");
  const emailInput = document.getElementById("contactEmail");
  const messageInput = document.getElementById("contactMessage");
  const submitButton = document.getElementById("contactSubmitButton");
  const session = getSession();

  if (session) {
    nameInput.value = session.name || "";
    emailInput.value = session.email || "";
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const payload = {
      name: nameInput.value.trim(),
      email: emailInput.value.trim(),
      message: messageInput.value.trim()
    };

    if (!payload.name || !payload.email || !payload.message) {
      showMessage("contactStatus", "Please fill all contact fields.", "error");
      return;
    }

    if (!validateEmail(payload.email)) {
      showMessage("contactStatus", "Please enter a valid email address.", "error");
      return;
    }

    submitButton.disabled = true;
    submitButton.textContent = "Sending...";

    try {
      const response = await request("/contact", {
        method: "POST",
        body: JSON.stringify(payload)
      });
      showMessage("contactStatus", response || "Your message has been sent.", "success");
      messageInput.value = "";
    } catch (error) {
      showMessage("contactStatus", error.message, "error");
    } finally {
      submitButton.disabled = false;
      submitButton.textContent = "Contact us";
    }
  });
});
