
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
import {Payment} from "./components/Payment.jsx";

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
              <Route path="/reset-password" element={<ResetPassword />} />
              <Route path={"/create-account"} element={<Signup />} />
              <Route path={"/account"} element={<AccountPage/>} />
              <Route path={"/landing"} element={<Landing />} />
              <Route path={"/payment"} element={<Payment />} />
          </Routes>
      </BrowserRouter>
  );
}

export default App;
