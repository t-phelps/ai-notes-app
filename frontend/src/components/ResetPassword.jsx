import {useState} from "react";
import "../styles/Global.css";
import {House} from "lucide-react";
import {useNavigate} from "react-router-dom";
import * as yup from "yup";
export const ResetPassword = () => {

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

    const handleEmailChange = async() => {

        try{
            await schema.validate({email});


        }catch(err){
            console.log(err);
        }
    }

    return (
        <div className="regular-container">
            <form className="regular-form" onSubmit={handleEmailChange}>
                <h2>Reset Password</h2>
                <label className={"regular-label"} htmlFor="Email">Email:</label>
                <input className={"regular-input"} type="text" placeholder="Email..." id="username" onChange={(e) => setEmail(e.target.value)}/>
                {/*<button className={"lucide-button"} onClick={navigate("/")}>Submit</button>*/}
                <button><House /></button>
            </form>

        </div>
    );
}