
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

function App() {

   const handleCreateSession = async () => {
       try {
           const response = await fetch("http://localhost:8080/stripe/create-checkout-session?lookup_key=test_key_1", {
               method: "POST",
               body: JSON.stringify({
                   lookup_key: "test_key_1",
               }),
               headers: {
                   "Content-Type": "application/json",
               },
           });

           const data = await response.json();
           if (data.url) {
               // redirect to url given by stripe
               window.location.href = data.url;
           } else if (data.error) {
               console.log("Stripe error", data.error);
               throw new Error("Error creating Stripe session");
           }
       }catch(err){
           console.error(err);
       }
    }
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
              <Route path="/reset-password" element={<ResetPassword/>} />
              <Route path={"/create-account"} element={<Signup/>} />
              <Route path={"/account"} element={<AccountPage/>} />
              <Route path={"/landing"} element={<Landing/>} />
          </Routes>
      </BrowserRouter>
  );
}

export default App;
