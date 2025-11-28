import "../styles/SignupStyle.css";
import {useState} from "react";
import * as yup from "yup";
import {Link} from "react-router-dom";
import {useNavigate} from "react-router-dom";

export const Signup = () => {
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [error, setError] = useState("");

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
        setError("");
        try{
            // validate schema from yup
            await schema.validate({username, email, password, confirmPassword});

            const response = await fetch("http://localhost:8080/auth/create", {
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

            const responseBody = await response.text();
            console.log("Response body:", responseBody);
            if (!response.ok) throw new Error(responseBody);

            console.log("Signup successful");
            navigate("/landing");
        }catch(error){
            setError(error.errors.join(", "));
            console.log(error);
        }

    }

    return(
        <div className="signup-container">
            <div className="signup-box">
                <h2>Welcome to Notes-AI</h2>
                <form className="signup-form" onSubmit={handleSignup}>
                    {error && <p className="error">{error}</p>}
                    <label className={"signup-label"} id={"email"}>Email:</label>
                    <input className={"signup-input"} type={"text"} id={"email"} placeholder={"Email address"} onChange={(e) => setEmail(e.target.value)} />

                    <label className={"signup-label"} id={"username"}>Username:</label>
                    <input className={"signup-input"} type={"text"} id={"username"} placeholder={"Username"} onChange={(e) => setUsername(e.target.value)} />

                    <label className={"signup-label"} id={"password"}>Password:</label>
                    <input className={"signup-input"} type={"password"} id={"password"} placeholder={"Password"} onChange={(e) => setPassword(e.target.value)} />

                    <label className={"signup-label"} id={"confirm-password"}>Confirm Password:</label>
                    <input className={"signup-input"} type={"password"} id={"confirm-password"} placeholder={"Confirm Password"} onChange={(e) => setConfirmPassword(e.target.value)} />

                    <div className={"signup-button"}><button type="submit">Submit</button></div>
                </form>
                <div>
                    <p>Already have an account? <Link to={"/"}>Login!</Link></p>
                </div>
            </div>
        </div>
    );
}