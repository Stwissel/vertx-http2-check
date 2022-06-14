// Once DOM is loaded hook into buttons and form
if (document.readyState != 'loading') {
    bootstrapForm();
} else {
    document.addEventListener('DOMContentLoaded', bootstrapForm);
}

function bootstrapForm() {
    var button1 = document.getElementById("button1");
    var button2 = document.getElementById("button2");

    button1.addEventListener("click", function(event) {
        event.preventDefault();
        const data = {
            username: "John Doe",
            password: "password"
        };
        sendit(data);
    });

    button2.addEventListener("click", function(event) {
        event.preventDefault();
        const data = {
            username: "John Doe",
            password: "john"
        };
        sendit(data);
    });
}

function sendit(data) {
    const msg1 = document.getElementById("message1");
    const msg2 = document.getElementById("message2");
    msg2.innerHTML = "";
    document.body.style.cursor = "wait";
    fetch("/api/auth", {
        method: "POST", // or 'PUT'
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(data)
    })
        .then((response) => {
            if (!response.ok) {
                const errMsg = "HTTP error, status = " + response.status;
                msg2.innerHTML = errMsg;
            }
            return response.text();
        })
        .then((datax) => {
            document.body.style.cursor = "default";
            msg1.innerHTML = datax;
        })
        .catch((error) => {
            document.body.style.cursor = "default";
            msg1.innerHTML = error.message;
        });
}