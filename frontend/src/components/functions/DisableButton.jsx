export function disableButton(e) {

    const button = e.currentTarget;

    button.disabled = true;

    setTimeout(function() {
        button.disabled = false;
    }, 120000);

    console.log("Password reset requested. Button disabled for 2 minutes");
}