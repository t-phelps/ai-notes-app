import { useEffect, useState } from "react";
import "../styles/AccountStyle.css";
import * as yup from "yup";
import { useNavigate} from "react-router-dom";
import { NavBar } from "./NavBar.jsx";
import streamDownloadToFile from "./functions/StreamDownloadToFile";
import retryAuth from "./functions/retryAuth";
import moment from "moment";
import BASE_URL from "../config.js";

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
    const[ clusteredNotes, setClusteredNotes] = useState(() => new Map());
    const [loading, setLoading] = useState(false);


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
                const options = {
                    credentials: "include",
                };

                // TODO this could be 1 query with a join instead of 2 DB calls
                const purchaseHistory = await retryAuth(`${BASE_URL}/account/purchase-history`, options);
                const userDetails = await retryAuth(`${BASE_URL}/account/user-details`, options);
                const notes = await retryAuth(`${BASE_URL}/notes/fetch-clustered-notes`, options);

                if(!purchaseHistory.ok || !userDetails.ok){
                    throw new Error("Fetching user info failed");
                }

                const historyData = await purchaseHistory.json();
                const userData = await userDetails.json();
                const userNotes = await notes.json();

                const formattedHistoryData = historyData
                    .filter(item => item.status !== null) // remove orphaned status rows
                    .map((item) => ({
                        status: item.status,
                        subscriptionPeriod:
                            moment(item.current_period_end)
                                .diff(moment(), "days")
                    }));

                setUsername(userData.username);
                setEmail(userData.email);

                setSubscription(formattedHistoryData);

                const map = new Map(
                    Object.entries(userNotes).map(([key, value]) => [
                        key,
                        new Set(value)
                    ])
                );

                setClusteredNotes(map);


            }catch(err){
                console.error(err)
            }
        };

        fetchData();
    }, []);

    const handlePasswordChange = async (e) => {
        e.preventDefault();
        setLoading(true);
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
            const response = await retryAuth(`${BASE_URL}/account/change-password`, options);

            if (!response.ok) throw new Error("Failed to change password");

            setOldPassword("");
            navigate("/");
        } catch (error) {

            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    const handleAccountDelete = async (e) => {
        e.preventDefault();
        setLoading(true);
        if (deletePassword !== confirmDeletePassword) {
            console.error("Passwords do not match");
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
            const response = await retryAuth(`${BASE_URL}/account/delete`, options);

            if (!response.ok) throw new Error("Failed to delete account");
            navigate("/");
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    const downloadDocument = async (title) => {
        try{
            setLoading(true);
            const options = {
                method: "POST",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    title: title
                })
            }
            let response = await retryAuth(`${BASE_URL}/notes/download-note`, options);

            await streamDownloadToFile(response, title + ".txt")
        }catch(err){
            console.error(err);
        } finally {
            setLoading(false);
        }
    }

    function renderClusters(map) {
        const elements = [];

        for (const [noteId, noteSet] of map) {
            elements.push(
                <div key={noteId}>
                    <strong>{noteId}</strong>
                    <div>
                        {[...noteSet].map((item) => (
                            <div key={item}>
                                <div>Title: {item}</div>
                                <button
                                    onClick={() => downloadDocument(item)}
                                    disabled={loading}>
                                    {loading ? "Downloading..." : "Download"}
                                </button>
                            </div>
                        ))}
                    </div>
                </div>
            );
        }
        return elements;
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

                            <button
                                className="change-pwd-btn"
                                type="submit"
                                disabled={loading}>
                                {loading ? "Changing..." : "Change Password"}
                            </button>
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

                            <button
                                className="account-delete-btn"
                                type="submit"
                                disabled={loading}>
                                {loading ? "Deleting..." : "Delete"}
                            </button>
                        </form>
                    </div>
                </div>

                <div className="account-history">
                    <h2>Notes History</h2>

                    {clusteredNotes.size === 0 ? (
                        <p>No notes yet.</p>
                    ) : (
                        <>
                            {renderClusters(clusteredNotes)}
                        </>
                        )
                    }
                </div>

                <div className="purchase-history">
                    <h2>Purchase History</h2>

                    {subscription.length === 0 ? (
                        <p>No purchase history.</p>
                    ) : (
                        subscription.map((item, index) => (
                            <div key={index}>
                                <p>Status: {item?.status}</p>
                                <p>Subscription Expires: {item.subscriptionPeriod} days</p>
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
};
