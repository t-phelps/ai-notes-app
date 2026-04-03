let refreshPromise = null;

const retryAuth = async (url, options = {}) => {
    try {
        let response = await fetch(url, options);

        if (response.status !== 401) {
            return response;
        }

        // If no refresh is currently happening, start one
        if (!refreshPromise) {
            refreshPromise = fetch("${BASE_URL}/auth/refresh", {
                method: "POST",
                credentials: "include"
            }).then(res => {
                if (!res.ok) {
                    // TODO force logout
                    throw new Error("Refresh failed");
                }
                return res;
            }).finally(() => {
                refreshPromise = null;
            });
        }

        // Wait for refresh to finish
        await refreshPromise;

        // Retry original request
        return fetch(url, options);

    } catch (err) {
        console.error("Request failed:", err.message);
        throw err;
    }
};

export default retryAuth;