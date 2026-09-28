let state = null;
let applicationStarted = false;
let questionMode = false;

const $ = (id) => document.getElementById(id);

async function api(url, options = {}) {
    const response = await fetch(url, {
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        },
        ...options
    });

    let body = {};

    try {
        body = await response.json();
    } catch {
        body = {};
    }

    if (!response.ok) {
        throw new Error(
            body.error || `Request failed with status ${response.status}`
        );
    }

    return body;
}


/* ============================================================
   MESSAGE HANDLING
   ============================================================ */

function addMessage(who, text) {
    if (!text) {
        return;
    }

    const messages = $("messages");

    if (!messages) {
        return;
    }

    const message = document.createElement("div");
    message.className = `msg ${who}`;

    const bubble = document.createElement("div");
    bubble.className = "bubble";
    bubble.textContent = text;

    message.appendChild(bubble);
    messages.appendChild(message);

    requestAnimationFrame(() => {
        messages.scrollTop = messages.scrollHeight;
    });
}


/* ============================================================
   CURRENT QUESTION
   ============================================================ */

function getCurrentQuestion() {
    if (!state || !Array.isArray(state.journey)) {
        return null;
    }

    return (
        state.journey[state.current_question_index] ||
        state.journey[0] ||
        null
    );
}


/* ============================================================
   VIEW MANAGEMENT
   ============================================================ */

function showChatView() {
    $("homeView").classList.add("hidden");
    $("chatView").classList.remove("hidden");

    setTimeout(() => {
        $("input").focus();
    }, 50);
}

function showHomeView() {
    $("chatView").classList.add("hidden");
    $("homeView").classList.remove("hidden");

    $("messages").innerHTML = "";
    $("questionControls").innerHTML = "";
    $("input").value = "";

    updateSendButton();
}


/* ============================================================
   QUESTION CONTROLS
   ============================================================ */

function renderQuestionControls() {
    const panel = $("questionControls");

    if (!panel) {
        return;
    }

    panel.innerHTML = "";

    if (!applicationStarted || questionMode) {
        return;
    }

    const question = getCurrentQuestion();

    if (!question) {
        return;
    }

    /*
     * Detect DOB by both type and question id.
     * This protects the UI if the backend defines DOB
     * as "text" instead of "dob".
     */
    const isDobQuestion =
        question.type === "dob" ||
        question.id === "dob" ||
        question.id === "date_of_birth";

    if (isDobQuestion) {
        renderDateControl(panel);
        return;
    }

    if (
        question.type === "choice" &&
        Array.isArray(question.options)
    ) {
        renderChoiceControl(panel, question.options);
        return;
    }

    if (question.type === "terminal") {
        renderChoiceControl(
            panel,
            Array.isArray(question.options)
                ? question.options
                : []
        );
    }
}


/* ============================================================
   DATE CONTROL
   ============================================================ */

function renderDateControl(panel) {
    const group = document.createElement("div");
    group.className = "dob-group";

    const label = document.createElement("label");
    label.className = "dob-label";
    label.textContent = "SELECT DATE OF BIRTH";
    label.setAttribute("for", "dobInput");

    const input = document.createElement("input");

    input.id = "dobInput";
    input.className = "dob-input";
    input.type = "date";

    const today = new Date();

    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, "0");
    const day = String(today.getDate()).padStart(2, "0");

    input.max = `${year}-${month}-${day}`;

    input.addEventListener("change", async () => {
        if (!input.value) {
            return;
        }

        const [selectedYear, selectedMonth, selectedDay] =
            input.value.split("-");

        const formattedDate =
            `${selectedDay}-${selectedMonth}-${selectedYear}`;

        input.disabled = true;

        await sendMessage(formattedDate);
    });

    const note = document.createElement("p");
    note.className = "control-note";
    note.textContent =
        "Select your date of birth using the calendar.";

    group.appendChild(label);
    group.appendChild(input);
    group.appendChild(note);

    panel.appendChild(group);
}


/* ============================================================
   CHOICE CONTROL
   ============================================================ */

function renderChoiceControl(panel, options) {
    if (!options.length) {
        return;
    }

    const group = document.createElement("div");
    group.className = "choice-group";

    const label = document.createElement("div");
    label.className = "choice-label";
    label.textContent = "CHOOSE AN OPTION";

    const buttons = document.createElement("div");
    buttons.className = "choice-buttons";

    options.forEach((option) => {
        const button = document.createElement("button");

        button.type = "button";
        button.className = "choice-button";
        button.textContent = option;

        button.addEventListener("click", async () => {
            disableChoiceButtons(buttons);
            await sendMessage(option);
        });

        buttons.appendChild(button);
    });

    group.appendChild(label);
    group.appendChild(buttons);

    panel.appendChild(group);
}

function disableChoiceButtons(container) {
    const buttons = container.querySelectorAll("button");

    buttons.forEach((button) => {
        button.disabled = true;
        button.style.opacity = "0.55";
        button.style.cursor = "default";
    });
}


/* ============================================================
   SESSION
   ============================================================ */

async function createSession() {
    const response = await api("/api/session");

    state = response.snapshot;

    return response;
}


/* ============================================================
   START APPLICATION
   ============================================================ */

async function startApplication() {
    try {
        applicationStarted = true;
        questionMode = false;

        $("messages").innerHTML = "";
        $("questionControls").innerHTML = "";

        const response = await createSession();

        showChatView();

        addMessage(
            "bot",
            response.welcome ||
            "Before we get started, may I have your name?"
        );

        renderQuestionControls();

    } catch (error) {
        applicationStarted = false;

        showChatView();

        addMessage(
            "bot",
            error.message ||
            "Unable to start the application."
        );
    }
}


/* ============================================================
   INSURANCE QUESTION MODE
   ============================================================ */

async function startQuestionMode() {
    try {
        applicationStarted = false;
        questionMode = true;

        $("messages").innerHTML = "";
        $("questionControls").innerHTML = "";

        const response = await createSession();

        showChatView();

        addMessage(
            "bot",
            "Of course. What would you like to know about health insurance?"
        );

        /*
         * Do not render underwriting controls in question mode.
         */
        renderQuestionControls();

    } catch (error) {
        applicationStarted = false;
        questionMode = false;

        showChatView();

        addMessage(
            "bot",
            error.message ||
            "Unable to open the assistant."
        );
    }
}


/* ============================================================
   SEND MESSAGE
   ============================================================ */

async function sendMessage(text) {
    const message = (text || "").trim();

    if (!message || !state) {
        return;
    }

    addMessage("user", message);

    $("input").value = "";

    updateSendButton();

    /*
     * Remove the current choice/date control immediately.
     * This prevents the user from submitting multiple answers
     * while the backend is processing the previous one.
     */
    $("questionControls").innerHTML = "";

    try {
        const endpoint = questionMode
            ? "/api/insurance-question"
            : "/api/chat";

        const response = await api(endpoint, {
            method: "POST",

            body: JSON.stringify({
                session_id: state.session_id,
                message: message
            })
        });

        if (response.reply) {
            addMessage("bot", response.reply);
        }

        if (response.snapshot) {
            state = response.snapshot;
        }

        /*
         * In application mode, continue rendering the next
         * underwriting question.
         */
        if (applicationStarted && !questionMode) {
            applicationStarted = true;
        }

        renderQuestionControls();

    } catch (error) {
        addMessage(
            "bot",
            `Something went wrong: ${error.message}`
        );

        /*
         * If sending failed, render the current control again
         * so the user isn't stuck.
         */
        renderQuestionControls();
    }
}


/* ============================================================
   MENU
   ============================================================ */

function toggleMenu() {
    const panel = $("menuPanel");
    const button = $("menuButton");

    if (!panel || !button) {
        return;
    }

    const isOpen = !panel.classList.contains("hidden");

    panel.classList.toggle("hidden", isOpen);

    button.setAttribute(
        "aria-expanded",
        String(!isOpen)
    );
}


/* ============================================================
   RESET APPLICATION
   ============================================================ */

async function resetApplication() {
    try {
        if (state?.session_id) {
            await api("/api/reset", {
                method: "POST",

                body: JSON.stringify({
                    session_id: state.session_id
                })
            });
        }

        state = null;

        applicationStarted = false;
        questionMode = false;

        $("messages").innerHTML = "";
        $("questionControls").innerHTML = "";

        showHomeView();

        closeMenu();

    } catch (error) {
        addMessage(
            "bot",
            `Unable to restart: ${error.message}`
        );
    }
}


/* ============================================================
   MENU CLOSE
   ============================================================ */

function closeMenu() {
    const panel = $("menuPanel");
    const button = $("menuButton");

    if (!panel || !button) {
        return;
    }

    panel.classList.add("hidden");

    button.setAttribute(
        "aria-expanded",
        "false"
    );
}


/* ============================================================
   SEND BUTTON
   ============================================================ */

function updateSendButton() {
    const input = $("input");
    const button = $("sendButton");

    if (!input || !button) {
        return;
    }

    button.classList.toggle(
        "active",
        input.value.trim().length > 0
    );
}


/* ============================================================
   CHAT HEADER COLLAPSE
   ============================================================ */

function setupChatCollapse() {
    const chatShell = document.querySelector(".chat-shell");
    const chatHeader = $("chatHeader");

    if (!chatShell || !chatHeader) {
        return;
    }

    chatHeader.addEventListener("click", (event) => {

        /*
         * Do not collapse when the menu button is clicked.
         */
        if (event.target.closest("#menuButton")) {
            return;
        }

        /*
         * Do not collapse when clicking inside the menu.
         */
        if (event.target.closest("#menuPanel")) {
            return;
        }

        chatShell.classList.toggle("chat-collapsed");
    });
}


/* ============================================================
   EVENT LISTENERS
   ============================================================ */

$("startApplicationBtn").addEventListener(
    "click",
    startApplication
);

$("askQuestionBtn").addEventListener(
    "click",
    startQuestionMode
);

$("menuButton").addEventListener(
    "click",
    (event) => {
        event.stopPropagation();
        toggleMenu();
    }
);

$("resetBtn").addEventListener(
    "click",
    (event) => {
        event.stopPropagation();
        resetApplication();
    }
);

$("chatForm").addEventListener(
    "submit",
    (event) => {
        event.preventDefault();

        sendMessage($("input").value);
    }
);

$("input").addEventListener(
    "input",
    updateSendButton
);

$("input").addEventListener(
    "keydown",
    (event) => {
        if (event.key === "Enter") {
            event.preventDefault();

            $("chatForm").requestSubmit();
        }
    }
);


document.addEventListener("DOMContentLoaded", () => {
    setupChatCollapse();
    updateSendButton();
});