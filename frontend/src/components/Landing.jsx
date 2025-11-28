import {NavBar} from "./NavBar.jsx";
import "../styles/LandingStyle.css";
import {useState} from "react";

export const Landing = () => {

    const [title, setTitle] = useState("");
    const [text, setText] = useState("");

    const saveNoteToCloud = async (e) => {
        e.preventDefault();
        try {
            const response = await fetch("http://localhost:8080/notes/to-cloud", {
                method: "POST",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    title: title,
                    notes: text,
                }),
            });

            if (!response.ok) {
                console.log("Failed with status: ", response.status);
                throw new Error(response.statusText);
            }

            console.log("Successfully saved to cloud");

        }catch(error){
            console.log("Error: ", error.message);
        }
    }

    const generateStudyGuide = async (e) => {
        e.preventDefault();
        try {
            const response = await fetch("http://localhost:8080/study/generate-study-guide", {
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