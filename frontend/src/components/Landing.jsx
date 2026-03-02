import {NavBar} from "./NavBar.jsx";
import "../styles/LandingStyle.css";
import {useEffect, useState} from "react";

export const Landing = () => {

    const [title, setTitle] = useState("");
    const [text, setText] = useState("");
    const [error, setError] = useState("");

    const saveNoteToCloud = async (e) => {
        e.preventDefault();

        if (!text && !title) {
            setError("Please enter a title or notes");
            return;
        }

        try {
            let response = await fetch("http://localhost:8080/notes/to-cloud", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({ title, notes: text }),
            });

            if(response.status === 401){
                const newResponse = await fetch("http://localhost:8080/auth/refresh", {
                    method: "POST",
                    credentials: "include",
                });

                if(!newResponse.ok){
                    setError("Failed to refresh token");
                    return;
                }

                response = await fetch("http://localhost:8080/notes/to-cloud", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    body: JSON.stringify({ title, notes: text }),
                });
            }

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                setError(errorData.message || `Request failed with status ${response.status}`);
                return;
            }

            setError("");
            alert("Successfully Saved To The Cloud!");
            setTitle("");
            setText("");
            window.location.reload();
        } catch (err) {
            console.error("Request failed:", err);
            setError(err.message || "Network Error. Please try again.");
        }
    };

    const generateStudyGuide = async (e) => {
        e.preventDefault();

        try {
            const response = await fetch("http://localhost:8080/notes/generate-study-guide", {
                method: "POST",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    notes: text,
                }),
            });

            if (!response.ok) {
                console.log("Failed with status: ", response.status);
                throw new Error(response.statusText);
            }

            const data = await response.json();
            console.log("Data received: ", data);

            console.log("Generated notes");
        }catch(error){
            console.log("Error: ", error.message);
        }
    }

    return (
        <div>
            <NavBar />
                <div className={"landing-container"}>
                    <div className={"main-section"}>
                        <div className={"create-notes"}>
                            <label htmlFor="title">
                                Title:
                            </label>
                            <input
                                name={"title"}
                                placeholder="Title"
                                onChange={(e) => setTitle(e.target.value)} />

                            <textarea
                                name={"text"}
                                placeholder="Notes..."
                                onChange={(e) => setText(e.target.value)} />

                            {/* ERROR MESSAGE */}
                            {error && (
                                <p style={{ color: "red", marginTop: "10px" }}>
                                    {error}
                                </p>
                            )}

                            <button
                                className="create-note-button"
                                type="button"
                                onClick={saveNoteToCloud}>
                                Save
                            </button>


                            <button
                                className={"generate-study-guide-button"}
                                type="button"
                                onClick={generateStudyGuide}>
                                Generate Study Guide
                            </button>
                        </div>
                    </div>
                </div>
        </div>
    );
}