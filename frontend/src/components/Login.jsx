import {useState } from "react";
import * as yup from "yup";
import { useNavigate, Link } from "react-router-dom";
import "../styles/LoginStyle.css";
import "../styles/Global.css";
import BASE_URL from "../config.js";
import {Eye} from "lucide-react";


export const LoginComponent = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [isVisible, setVisible] = useState(false);

    const schema = yup.object().shape({
        username: yup.string().required("Username is required"),
        password: yup.string().required("Password is required"),
    });

    const toggleVisibility = () => {
        setVisible(!isVisible);
    }

    /**
     * handle
     * @param e
     * @returns {Promise<void>}
     */
    const handleLoginSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            await schema.validate({username, password}); // throws error if not valid

            const response = await fetch(`${BASE_URL}/auth/login`, {
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

           if(!response.ok) {
               let msg;
               switch(response.status){
                   case 401:
                       msg = "Invalid Credentials";
                       break;
                   case 500:
                       msg = "Server Error";
                       break;
                   default:
                       msg = "Something went wrong. Try Again!"
               }
               setError(msg);
               return;
           }

            navigate("/landing");
        }catch(err) {
            if(err.name === "ValidationError") {
                setError(err.message);
            }else{
                console.error(err);
                setError("Something went wrong. Please try again.")
            }

        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="regular-container">
            <form className="regular-form" onSubmit={handleLoginSubmit}>
                <h2>Welcome To Notes-AI</h2>
                {error && <div className={"error-message"}>{error}</div>}
                <label className={"regular-label"} htmlFor="username">Username:</label>
                <input className={"regular-input"}
                       type="text"
                       placeholder="Username"
                       id="username"
                       value={username}
                       onChange={(e) => {
                           setUsername(e.target.value);
                            setError("");
                       }}/>
                <label className={"regular-label"} htmlFor="password">Password:</label>
                <div className={"password-inline"}>
                    <input className={"regular-input"}
                           type={isVisible ? "text" : "password"}
                           placeholder="Password"
                           id="password"
                           value={password}
                           onChange={(e) => {
                               setPassword(e.target.value);
                               setError("");
                           }} />
                    <button type={"button"}
                            onClick={toggleVisibility}
                            className={"password-inline-button"}>
                        <Eye />
                    </button>
                </div>
                <button
                    className={"regular-button"}
                    type="submit"
                    disabled={loading}>{loading ? "Logging In..." : "Login"}
                </button>
                <div>
                    <p>Forgot Password? <Link to="/reset-password">Reset!</Link></p>
                    <p className="or-message">------------- or -------------</p>
                    <p className="create-account">Not a user? <Link to="/create-account">Register Today!</Link></p>
                </div>
            </form>
        </div>
    );
}