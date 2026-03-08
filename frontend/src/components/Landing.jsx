import {NavBar} from "./NavBar.jsx";
import "../styles/LandingStyle.css";
import {useEffect, useState} from "react";
import streamDownloadToFile from "./functions/StreamDownloadToFile";
import retryAuth from "./functions/retryAuth";

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
            const options = {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({ title, notes: text }),
            };

            let response = await retryAuth("http://localhost:8080/notes/to-cloud", options);

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                setError(errorData.message || `Request failed with status ${response.status}`);
                return;
            }

            alert("Successfully Saved To The Cloud!");
        } catch (err) {
            console.error("Request failed:", err);
            setError(err.message || "Network Error. Please try again.");
        }
    };

    const generateStudyGuide = async (e) => {
        e.preventDefault();

        try {
            const options = {
                method: "POST",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    title,
                    notes: text,
                }),
            };
            let response = await fetch("http://localhost:8080/notes/generate-study-guide", options);


            if (!response.ok) {
                console.log("Failed with status: ", response.status);
                throw new Error(response.statusText);
            }

            await streamDownloadToFile(response, "studyGuide.txt");

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