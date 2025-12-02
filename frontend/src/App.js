
import {
    BrowserRouter,
    Route,
    Routes,
    Navigate,
} from "react-router-dom";
import {LoginComponent} from "./components/Login.jsx";
import {ResetPassword} from "./components/ResetPassword.jsx";
import {Signup} from "./components/Signup.jsx";
import {AccountPage} from "./components/AccountPage.jsx";
import {Landing} from "./components/Landing.jsx";
import { useState} from "react";
import {Payment} from "./components/Payment.jsx";

function App() {

    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [userNotesArray, setUserNotesArray] = useState([]);

  return (
    // <div className="App">
    //     {/*<button onClick={handleCreateSession}>*/}
    //     {/*    Create Session*/}
    //     {/*</button>*/}
    //
    // </div>
      <BrowserRouter>
          <Routes>
              <Route path="/" element={<LoginComponent />} />
              <Route path="/reset-password" element={<ResetPassword />} />
              <Route path={"/create-account"} element={<Signup />} />
              <Route path={"/account"} element={<AccountPage username={username} email={email} userNotesArray={userNotesArray} />} />
              <Route path={"/landing"} element={<Landing setUsername={setUsername}
               setEmail={setEmail} setUserNotesArray={setUserNotesArray} />} />
              <Route path={"/payment"} element={<Payment />} />
          </Routes>
      </BrowserRouter>
  );
}

export default App;
