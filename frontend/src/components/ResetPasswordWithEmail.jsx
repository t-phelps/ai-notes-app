import {useState} from "react";
import "../styles/Global.css";
import {disableButton} from "../components/functions/DisableButton.jsx";
import {useNavigate} from "react-router-dom";
import * as yup from "yup";
export const ResetPasswordWithEmail = () => {

    const [email, setEmail] = useState("");
    const navigate = useNavigate();

    const schema = yup.object().shape({
        email: yup
            .string()
            .email("This is not a valid email!")
            .matches(
                /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
                "Email must be a valid address with a domain"
            )
            .required("Your Email Is Required")
    });

    const handleEmailChange = async(e) => {
        e.preventDefault();
        try{
            await schema.validate({email});

            alert("Email will be sent if the account exists");

            const response = await fetch("${BASE_URL}/mail/reset-password", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    email: email,
                }),
            });

            // dont let users see this for security reasons
            if(!response.ok){
                console.log(response.status);
                return;
            }

        }catch(err){
            console.log(err);
        }
    }

    return (
        <div className="regular-container">
            <form className="regular-form" onSubmit={(e) => {
                disableButton(e);
                handleEmailChange(e);
            }}>
                <h2>Reset Password</h2>
                <label className={"regular-label"} htmlFor="Email">Email:</label>
                <input className={"regular-input"} type="text" placeholder="Email..." id="username" onChange={(e) => setEmail(e.target.value)}/>
                <button className={"regular-button"} type={"submit"}>Submit</button>
            </form>

        </div>
    );
}