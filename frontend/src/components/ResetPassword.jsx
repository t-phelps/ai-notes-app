import "../styles/Global.css";
import {useState} from "react";
import * as yup from "yup";
import { useSearchParams} from "react-router-dom";
import {useNavigate} from "react-router-dom";

export const ResetPassword = () => {
    const schema = yup.object().shape({
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

    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const token = searchParams.get("token");

    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [error, setError] = useState("");

    const handlePasswordReset = async (e) => {
        e.preventDefault();
        try{
            await schema.validate({password, confirmPassword});

            if(!token){
                alert("Invalid token");
                return;
            }

            const response = await fetch("http://localhost:8080/account/reset-password", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    password: password,
                    uuid: token,
                }),
            })

            if(!response.ok) {
                alert("Password reset failed.");
                return;
            }

            alert("Reset password reset successfully.");
            navigate("/")
        }catch(err){
            console.log(err);
        }
    }

    return (
      <div className={"regular-container"}>
          <form className={"regular-form"} onSubmit={(e) => handlePasswordReset(e)}>
              <h2>Reset Your Password</h2>
              <label htmlFor={"password"}>Password: </label>
              <input className={"regular-input"} type={"password"} onChange={(e) => setPassword(e.target.value)}/>
              <label htmlFor={"confirm-password"}>Confirm Password: </label>
              <input className={"regular-input"} type={"confirm-password"} onChange={(e) => setConfirmPassword(e.target.value)}/>
              <button className={"regular-button"} type="submit">Reset Password</button>
              <p>{error && <p className="error">{error}</p>}</p>
          </form>
      </div>
    );
}