import BASE_URL from "../../config.js";

let refreshPromise = null;

const refreshOptions = {
    method: "POST",
    credentials: "include",
}
const retryAuth = async (url, options = {}) => {
    try {
        let response = await fetch(url, options);

        if (response.status !== 401) {
            return response;
        }

        if (!refreshPromise) {
            refreshPromise = fetch(`${BASE_URL}/auth/refresh`, refreshOptions)
                .then(async (res) => {
                if (!res.ok) {
                    await fetch(`${BASE_URL}/auth/logout`, refreshOptions);
                    window.location.href = "/";
                    throw new Error("Refresh failed");
                }
                return res;
            }).finally(() => {
                refreshPromise = null;
            });
        }

        await refreshPromise;

        return fetch(url, options);

    } catch (err) {
        throw err;
    }
};

export default retryAuth;