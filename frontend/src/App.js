
import {
    BrowserRouter,
    Route,
    Routes,
    Navigate,
} from "react-router-dom";
import {LoginComponent} from "./components/Login.jsx";
import {ResetPasswordWithEmail} from "./components/ResetPasswordWithEmail.jsx";
import {Signup} from "./components/Signup.jsx";
import {AccountPage} from "./components/AccountPage.jsx";
import {Landing} from "./components/Landing.jsx";
import {Payment} from "./components/Payment.jsx";
import {ResetPassword} from "./components/ResetPassword";

function App() {

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
              <Route path="/reset-password" element={<ResetPasswordWithEmail />} />
              <Route path={"/create-account"} element={<Signup />} />
              <Route path={"/account"} element={<AccountPage/>} />
              <Route path={"/landing"} element={<Landing />} />
              <Route path={"/payment"} element={<Payment />} />
              <Route path={"/reset"} element={<ResetPassword />} />
          </Routes>
      </BrowserRouter>
  );
}

export default App;
