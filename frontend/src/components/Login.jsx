import {useState } from "react";
import * as yup from "yup";
import { useNavigate } from "react-router-dom";
import "../styles/LoginStyle.css";
import GoogleButton from 'react-google-button';
import { Link } from "react-router-dom";

export const LoginComponent = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const navigate = useNavigate();

    const schema = yup.object().shape({
        username: yup.string().required("Username is required"),
        password: yup.string().required("Password is required"),
    });

    const handleGoogleLogin = () => {

    }

    /**
     * handle
     * @param e
     * @returns {Promise<void>}
     */
    const handleLoginSubmit = async (e) => {
        e.preventDefault(); // needed for a form login, if I go to submit the page the form will refresh the page and it won't get processed
        try {
            await schema.validate({username, password}); // throws error if not valid

            const response = await fetch("http://localhost:8080/auth/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                credentials: "include",
                body: JSON.stringify({
                    username: username,
                    password: password,
                })
            });

            if(!response.ok){
                alert("Invalid Credentials");
                throw new Error(response.statusText);
            }

            console.log("User logged in successfully");
            navigate("/account");
        }catch(err) {
            console.log("Error occurred while logging in: ", err);
        }
    }

    return (
        <div className="login-container">
            <form className="login-form" onSubmit={handleLoginSubmit}>
                <h2>Welcome To Notes-AI</h2>
                <label className={"login-label"} htmlFor="username">Username:</label>
                <input className={"login-input"} type="text" placeholder="Username" id="username" value={username} onChange={(e) => setUsername(e.target.value)} />
                <label className={"login-label"} htmlFor="password">Password:</label>
                <input className={"login-input"} type="password" placeholder="Password"  id="password" value={password} onChange={(e) => setPassword(e.target.value)} />
                <button className={"login-button"} type="submit">Login</button>
                <p>Forgot Password? <Link to="/reset-password">Reset!</Link></p>
                <p className="or-message">------------- or -------------</p>
                <GoogleButton className="google-button" type="light" onClick={handleGoogleLogin} />
                <p className="create-account">Not a user? <Link to="/create-account">Register Today!</Link></p>
            </form>
        </div>
    );
}