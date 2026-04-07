import { toast } from "react-toastify";

const toastOptions = {
    position: "top-center",
    autoClose: 5000,
    hideProgressBar: false,
    closeOnClick: true,
    pauseOnHover: true,
    draggable: false,
    progress: undefined,
    theme: "light",
}

export const toastSuccess = (message) => {
    toast.success(message,toastOptions);
}

export const toastBadRequest = () => {
    toast.error("Invalid request. Please check your input and try again.", toastOptions);
}

export const toastUnauthorizedUser = (message) => {
    toast.error(message, toastOptions);
}

export const toastServerError = () => {
    toast.error('An error occurred, please try again!', toastOptions);
}