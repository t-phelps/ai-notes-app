import {NavBar} from "./NavBar.jsx";
import "../styles/LandingStyle.css";
import {useEffect, useState} from "react";
import streamDownloadToFile from "./functions/StreamDownloadToFile";
import retryAuth from "./functions/retryAuth";

export const Landing = () => {

    const [title, setTitle] = useState("");
    const [text, setText] = useState("");
    const [error, setError] = useState("");
    const [userNotesDataSet, setUserNotesDataSet] = useState(() => new Set());
    const [username, setUsername] = useState("");
    const max = 2000;
    const [loading, setLoading] = useState(false);

    let charRemaining = max - text.length;

    useEffect(() => {
        const fetchUserNotesData = async () => {
            try {
                const options = {
                    credentials: "include",
                }

                const userDetails = await retryAuth("http://localhost:8080/account/user-details", options);

                if (!userDetails.ok) {
                    console.error("Error fetching user details within useEffect");
                    return;
                }

                const userDetailsResponse = await userDetails.json();
                console.log(userDetailsResponse.username);
                console.log(userDetailsResponse);
                const formattedUserNotesSet = new Set(
                    userDetailsResponse.userNotesDto.map(note =>
                        note.pathToNote
                            .replace(`gdrive:/ai-notes/${userDetailsResponse.username}/`, "")
                            .replace(".txt", "")
                    )
                );

                console.log(formattedUserNotesSet);
                setUserNotesDataSet(formattedUserNotesSet);
                console.log("User notes accessed");

            }catch(err){
                console.error(err);
            }
        };

        fetchUserNotesData();
    }, []);

    const saveNoteToCloud = async (e) => {
        e.preventDefault();
        if(loading) return;
        if (!text || !title) {
            setError("Please enter a title or notes");
            return;
        }

        if(userNotesDataSet.has(title)){
            setError("Title already exists, please enter a new one");
            return;
        }

        setLoading(true);


        setUserNotesDataSet(prev => {
            const updated = new Set(prev);
            updated.add(title);
            return updated;
        });

        try {
            const options = {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({ title, notes: text }),
            };

            let response = await retryAuth("http://localhost:8080/notes/to-cloud", options);

            if (!response.ok) {
                rollbackSet(title);
                const errorData = await response.json().catch(() => ({}));
                setError(errorData.message || `Request failed with status ${response.status}`);
                return;
            }

            alert("Successfully Saved To The Cloud!");
        } catch (err) {
            rollbackSet(title);
            console.error("Request failed:", err);
            setError(err.message || "Network Error. Please try again.");
        } finally{
            setLoading(false);
        }
    };

    const rollbackSet = (title) => {
        setUserNotesDataSet(prev => {
            const updated = new Set(prev);
            updated.delete(title);
            return updated;
        });
    }

    const generateStudyGuide = async (e) => {
        e.preventDefault();

        if (!text || !title) {
            setError("Please enter a title or notes");
            return;
        }

        if(loading) return;
        setLoading(true);
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
            let response = await retryAuth("http://localhost:8080/notes/generate-study-guide", options);

            if(response.status === 403){
                alert("Unauthorized to utilize this feature. Please purchase a subscription");
                return;
            }
            if (!response.ok) {
                console.log("Failed with status: ", response.status);
                throw new Error(response.statusText);
            }

            await streamDownloadToFile(response, "studyGuide.txt");

            console.log("Generated notes");
        }catch(error){
            console.log("Error: ", error.message);
        } finally{
            setLoading(false);
        }
    }

    return (
        <div>
            <NavBar />
                <div className={"landing-container"}>
                    <div className={"main-section"}>
                        <div className={"create-notes"}>
                            <label htmlFor="title">
                                Create Your Notes Here:
                            </label>
                            <input
                                name={"title"}
                                placeholder=" Title"
                                onChange={(e) => setTitle(e.target.value)} />

                            <textarea
                                name={"text"}
                                placeholder=" Notes..."
                                maxLength={max}
                                onChange={(e) => setText(e.target.value)} />

                            {/* ERROR MESSAGE */}
                            {error && (
                                <p style={{ color: "red", marginTop: "10px" }}>
                                    {error}
                                </p>
                            )}
                            <p>
                                {/* Display the current count and the total limit */}
                                {text.length} / {max} characters entered
                            </p>
                            <p style={{ color: charRemaining < 0 ? 'red' : 'black' }}>
                                {charRemaining} characters remaining
                            </p>

                            <button
                                className="create-note-button"
                                type="button"
                                onClick={saveNoteToCloud}
                                disabled={loading}>
                                {loading ? "Saving..." : "Save"}
                            </button>


                            <button
                                className={"generate-study-guide-button"}
                                type="button"
                                onClick={generateStudyGuide}
                                disabled={loading}>
                                {loading ? "Generating..." : "Generate Study Guide"}
                            </button>
                        </div>
                    </div>
                </div>
        </div>
    );
}