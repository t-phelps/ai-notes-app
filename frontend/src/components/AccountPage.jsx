import {useEffect, useState} from "react";
import "../styles/AccountStyle.css";
import * as yup from "yup";
import {useNavigate} from "react-router-dom";

import {NavBar} from "./NavBar.jsx";



export const AccountPage = ({username, email, userNotesArray}) => {

    // problem here, state issue? why are these null
    console.log("Username: ", username, " Email: ",  email, " User Notes Array: ", userNotesArray);
    const navigate = useNavigate();

    const [oldPassword, setOldPassword] = useState("");
    const [confirmedOldPassword, setConfirmedOldPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmedNewPassword, setConfirmedNewPassword] = useState("");

    const [subscription, setSubscription] = useState([]);

    const schema = yup.object().shape({
        newPassword: yup
            .string()
            .min(6, "Password length must be at least 6 characters")
            .max(16, "Password length must not exceed 16 characters")
            .matches(/[A-Z]/, "Password must contain at least one uppercase letter")
            .matches(/[!@#$%^&*(),.?":{}|<>]/, "Password must contain at least one special character")
            .required("Password is required"),
        confirmNewPassword: yup
            .string()
            .oneOf([yup.ref("password"), null], "Passwords must match")
            .required(),
    });

    useEffect(() => {
        const GetPurchaseHistory = async () => {
            try {
                const response = await fetch("http://localhost:8080/account/purchase-history", {
                    method: "GET",
                    credentials: "include",

                });

                if (!response.ok) throw new Error(response.statusText);

                const data = await response.json();

                console.log("Data: ", data);
                const formattedData = data.map((item) => ({
                    ...item,
                    subscriptionPeriod: (Number(item.current_period_end) - Number(item.current_period_start)) / 86400
                }));

                setSubscription(formattedData);
            }catch(err) {
                console.log(err);
            }
        }

        GetPurchaseHistory();
    }, [])


    const handlePasswordChange = async (e) => {
        e.preventDefault();
        try {
            await schema.validate({newPassword, confirmedNewPassword});

            const response = await fetch("http://localhost:8080/account/change-password", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    },
                    credentials: "include",
                    body: JSON.stringify({
                        newPassword: newPassword,
                        oldPassword: oldPassword,
                    }),
                });

                if (!response.ok) {
                    throw new Error("Failed to change password");
                }

                console.log("Password changed successfully");
                setOldPassword("");
                navigate("/");
            }catch(error){
                console.log(error);
            }
        }

        const handleAccountDelete = async (e) => {
            e.preventDefault();
            try {
                const response = await fetch("http://localhost:8080/account/delete", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                    },
                    credentials: "include",
                    body: JSON.stringify({
                        password: confirmedOldPassword,
                    }),
                });

                if (!response.ok) {
                    throw new Error("Failed to delete user");
                }

                console.log("Successfully deleted account");
            }catch(error){
                console.log(error);
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

                      <div className={"account-change-password"}>
                          <h2>Change Your Password</h2>
                          <form onSubmit={handlePasswordChange} className="account-change-password">
                              <label htmlFor={"password"}>Current Password:</label>
                              <input
                                  type={"password"}
                                  name={"password"}
                                  placeholder={"Old Password"}
                                  onChange={(e) => setOldPassword(e.target.value)} />

                              <label htmlFor={"newPassword"}>New Password:</label>
                              <input
                                  type={"password"}
                                  name={"newPassword"}
                                  placeholder={"New Password"}
                                  onChange={(e) => setNewPassword(e.target.value)} />

                              <label htmlFor={"confirmedNewPassword"}>Current Password:</label>
                              <input
                                  type={"password"}
                                  name={"confirmedNewPassword"}
                                  placeholder={"Confirm New Password"}
                                  onChange={(e) => setConfirmedNewPassword(e.target.value)} />

                              <button className={"change-pwd-btn"} type="submit">Change Password</button>
                          </form>
                      </div>

                      <div className={"account-delete"}>
                          <h2>Delete your account</h2>
                          <p>Please note you can't undo this action</p>
                          <form className="account-delete-form" onSubmit={handleAccountDelete}>
                              <label htmlFor={"deleteAccountPassword"}>Password: </label>
                              <input type="password" name={"deleteAccountPassword"} placeholder={"Password"}
                              onChange={(e) => setOldPassword(e.target.value)} />

                              <label htmlFor={"confirmedDeleteAccountPassword"}>Confirm Password: </label>
                              <input type={"password"} name={"confirmedDeleteAccountPassword"} placeholder={"Confirm Password"}
                                     onChange={(e) => setConfirmedOldPassword(e.target.value)} />

                              <button className={"account-delete-btn"} type={"submit"}>Delete Account</button>
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
                                    <strong>{note.pathToNote}</strong>
                                    <span>Saved at: {note.savedAt.slice(0, 5)}</span>
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
                                    <p>Subscription period: {item.subscriptionPeriod} days</p>
                                </div>
                            )
                        ))}
                    </div>
                </div>
            </div>
        );

    }