import "../styles/Global.css";
import {useState} from "react";
import * as yup from "yup";
import {Link} from "react-router-dom";
import {useNavigate} from "react-router-dom";
import BASE_URL from "../config.js";
import {Eye} from "lucide-react";

export const Signup = () => {
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [isVisible, setVisible] = useState(false);

    const toggleVisibility = () => {
        setVisible(!isVisible);
    }

    // define our schema on the fields within our form
    const schema = yup.object().shape({
        email: yup
            .string()
            .email("This is not a valid email!")
            .matches(
                /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
                "Email must be a valid address with a domain"
            )
            .required("Your Email Is Required"),
        username: yup
            .string()
            .min(6, "Username length must be at least 6 characters")
            .required("Username is required"),
        password: yup
            .string()
            .min(6, "Password length must be at least 6 characters")
            .max(16, "Password length must not exceed 16 characters")
            .matches(/[A-Z]/, "Password must contain at least one uppercase letter")
            .matches(/[!@#$%^&*(),.?":{}|<>]/, "Password must contain at least one special character")
            .required("Password is required"),
        confirmPassword: yup
            .string()
            .oneOf([yup.ref("password"), null], "Passwords must match")
            .required(),
    });

    /**
     * Handle signup with an api call to backend service
     * @returns {Promise<void>}
     */
    const handleSignup = async (e) => {
        e.preventDefault();
        setLoading(true);
        try{
            // validate schema from yup
            await schema.validate({username, email, password, confirmPassword});

            const response = await fetch(`${BASE_URL}/auth/create`, {
                method: "POST",
                body: JSON.stringify({
                    email: email,
                    username: username,
                    password: password,
                }),
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "include" // include credentials to get a jwt
            });

            if (!response.ok) {
                let msg;
                switch (response.status) {
                    case 400:
                        msg = "Invalid Request Body";
                        break;
                    case 500:
                        msg = "Server Error";
                        break;
                    default:
                        msg = "Something went wrong. Try Again!";
                }
                setError(msg);
                return;
            }

            navigate("/landing");
        }catch(err){
            if(err.name === "ValidationError") {
                setError(err.message);
            }else{
                console.error(err);
            }
        }finally {
            setLoading(false);
        }
    }

    return (
        <div className="regular-container">
            <form className="regular-form" onSubmit={handleSignup}>
                <h2>Register Here</h2>

                {error && <div className="error-message">{error}</div>}

                <label className="regular-label" htmlFor="email">Email:</label>
                <input className="regular-input"
                       type="text"
                       id="email"
                       placeholder="Email address"
                       onChange={(e) => setEmail(e.target.value)} />

                <label className="regular-label" htmlFor="username">Username:</label>
                <input className="regular-input"
                       type="text"
                       id="username"
                       placeholder="Username"
                       onChange={(e) => setUsername(e.target.value)} />

                <label className="regular-label" htmlFor="password">Password:</label>
                <div className="password-inline">
                    <input className="regular-input"
                           type={isVisible ? "text" : "password"}
                           id="password"
                           placeholder="Password"
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

                <label className="regular-label" htmlFor="confirm-password">Confirm Password:</label>
                <div className="password-inline">
                    <input className="regular-input"
                           type={isVisible ? "text" : "password"}
                           id="confirm-password"
                           placeholder="Confirm Password"
                           onChange={(e) => {
                               setConfirmPassword(e.target.value);
                               setError("");
                           }} />
                    <button type={"button"}
                            onClick={toggleVisibility}
                            className={"password-inline-button"}>
                        <Eye />
                    </button>
                </div>

                <button
                    className="regular-button"
                    type="submit"
                    disabled={loading}
                >
                    {loading ? "Submitting..." : "Submit"}
                </button>

                <p>Already have an account? <Link to="/">Login!</Link></p>
            </form>
        </div>
    );
}