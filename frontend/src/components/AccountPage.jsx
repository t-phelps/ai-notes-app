import { useEffect, useState } from "react";
import "../styles/AccountStyle.css";
import * as yup from "yup";
import { useNavigate, useLocation } from "react-router-dom";
import { NavBar } from "./NavBar.jsx";
import streamDownloadToFile from "./functions/StreamDownloadToFile";
import retryAuth from "./functions/retryAuth";

export const AccountPage = () => {
    const navigate = useNavigate();

    // password change states
    const [oldPassword, setOldPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmedNewPassword, setConfirmedNewPassword] = useState("");

    // account delete states
    const [deletePassword, setDeletePassword] = useState("");
    const [confirmDeletePassword, setConfirmDeletePassword] = useState("");

    const [subscription, setSubscription] = useState([]);

    const[username, setUsername] = useState("");
    const[email, setEmail] = useState("");
    const[ userNotesArray, setUserNotesArray] = useState([]);

    const schema = yup.object().shape({
        newPassword: yup
            .string()
            .min(6, "Password must be at least 6 characters")
            .max(16, "Password must not exceed 16 characters")
            .matches(/[A-Z]/, "Password must contain at least one uppercase letter")
            .matches(/[!@#$%^&*(),.?":{}|<>]/, "Password must contain at least one special character")
            .required("Password is required"),
        confirmNewPassword: yup
            .string()
            .oneOf([yup.ref("newPassword")], "Passwords must match")
            .required("Confirm your password"),
    });


    useEffect(() => {
        const fetchData = async () => {
            try{
                const [purchaseHistory, userDetails] = await Promise.all([
                    fetch("http://localhost:8080/account/purchase-history", {
                        credentials: "include",
                    }),
                    fetch("http://localhost:8080/account/user-details", {
                        credentials: "include",
                    })
                ]);

                if(!purchaseHistory.ok || !userDetails.ok){
                    throw new Error("Fetching user info failed");
                }

                const historyData = await purchaseHistory.json();
                const userData = await userDetails.json();

                const formattedHistoryData = historyData.map((item) => ({
                    ...item,
                    subscriptionPeriod:
                        (Number(item.current_period_end) - Number(item.current_period_start))
                }));

                const userNotesList = userData?.userNotesDto || [];
                userNotesList.forEach((item) => {
                    item.pathToNote = item.pathToNote.replace("gdrive:/ai-notes/", "");
                });

                setUsername(userData.username);
                setEmail(userData.email);
                setUserNotesArray(userNotesList);

                setSubscription(formattedHistoryData);
            }catch(err){
                console.log(err)
            }
        };

        fetchData();
    }, []);

    const handlePasswordChange = async (e) => {
        e.preventDefault();
        try {
            await schema.validate({
                newPassword,
                confirmNewPassword: confirmedNewPassword,
            });

            const options = {
                method: "POST",
                credentials: "include",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    newPassword,
                    oldPassword,
                }),
            };
            const response = await retryAuth("http://localhost:8080/account/change-password", options);

            if (!response.ok) throw new Error("Failed to change password");

            console.log("Password changed successfully");
            setOldPassword("");
            navigate("/");
        } catch (error) {
            console.log(error);
        }
    };

    const handleAccountDelete = async (e) => {
        e.preventDefault();

        if (deletePassword !== confirmDeletePassword) {
            console.log("Passwords do not match");
            return;
        }

        try {
            const options = {
                method: "POST",
                credentials: "include",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    password: deletePassword,
                }),
            }
            const response = await retryAuth("http://localhost:8080/account/delete", options);

            if (!response.ok) throw new Error("Failed to delete account");

            console.log("Account deleted successfully");
            navigate("/");
        } catch (error) {
            console.log(error);
        }
    };

    const downloadDocument = async (path) => {
        try{
            const options = {
                method: "POST",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    path: path
                })
            }
            let response = await retryAuth(`http://localhost:8080/notes/download-note`, options);

            console.log("Starting download");
            await streamDownloadToFile(response, path + ".txt")
            console.log("Download complete");
        }catch(err){
            console.log(err);
        }
    }

    return (
        <div>
            <NavBar />

            <div className="account-container">

                <div className="account-section">
                    <div className="account-info">
                        <h2>Account Info</h2>
                        <p>Username: {username}</p>
                        <p>Email: {email}</p>
                    </div>

                    <div className="account-change-password">
                        <h2>Change Your Password</h2>

                        <form onSubmit={handlePasswordChange} className="account-change-password">
                            <label>Current Password:</label>
                            <input
                                type="password"
                                placeholder="Old Password"
                                onChange={(e) => setOldPassword(e.target.value)}
                            />

                            <label>New Password:</label>
                            <input
                                type="password"
                                placeholder="New Password"
                                onChange={(e) => setNewPassword(e.target.value)}
                            />

                            <label>Confirm New Password:</label>
                            <input
                                type="password"
                                placeholder="Confirm New Password"
                                onChange={(e) => setConfirmedNewPassword(e.target.value)}
                            />

                            <button className="change-pwd-btn" type="submit">Change Password</button>
                        </form>
                    </div>

                    <div className="account-delete">
                        <h2>Delete Your Account</h2>
                        <p>You can't undo this action.</p>

                        <form className="account-delete-form" onSubmit={handleAccountDelete}>
                            <label>Password:</label>
                            <input
                                type="password"
                                placeholder="Password"
                                onChange={(e) => setDeletePassword(e.target.value)}
                            />

                            <label>Confirm Password:</label>
                            <input
                                type="password"
                                placeholder="Confirm Password"
                                onChange={(e) => setConfirmDeletePassword(e.target.value)}
                            />

                            <button className="account-delete-btn" type="submit">Delete Account</button>
                        </form>
                    </div>
                </div>

                <div className="account-history">
                    <h2>Notes History</h2>

                    {userNotesArray.length === 0 ? (
                        <p>No notes yet.</p>
                    ) : (
                        userNotesArray.map((note) => (
                            <div key={note.pathToNote} className="note-card">
                                <strong>{note.pathToNote}</strong>{" "}
                                <span>Saved at: {note.savedAt?.slice(0, 5) ?? "??"}</span>
                                <button className="download-btn"
                                        type="submit"
                                        onClick={() => downloadDocument(note.pathToNote)}>Download
                                </button>
                            </div>
                        ))
                    )}
                </div>

                <div className="purchase-history">
                    <h2>Purchase History</h2>

                    {subscription.length === 0 ? (
                        <p>No purchase history.</p>
                    ) : (
                        subscription.map((item, index) => (
                            <div key={index}>
                                <p>Status: {item.status}</p>
                                <p>Subscription Period: {item.subscriptionPeriod} days</p>
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
};
