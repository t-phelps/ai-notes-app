import {
    BrowserRouter,
    Route,
    Routes,
} from "react-router-dom";
import { LoginComponent } from "./components/Login.jsx";
import { ResetPasswordWithEmail } from "./components/ResetPasswordWithEmail.jsx";
import { Signup } from "./components/Signup.jsx";
import { AccountPage } from "./components/AccountPage.jsx";
import { Landing } from "./components/Landing.jsx";
import { Payment } from "./components/Payment.jsx";
import { ResetPassword } from "./components/ResetPassword";
import { ProtectedRoute } from "./components/Protectedroute.jsx";
import {Legal} from "./components/Legal.jsx";

function App() {
    return (
        <BrowserRouter>
            <Routes>
                {/* Public routes */}
                <Route path="/" element={<LoginComponent />} />
                <Route path="/reset-password" element={<ResetPasswordWithEmail />} />
                <Route path="/create-account" element={<Signup />} />
                <Route path="/reset" element={<ResetPassword />} />
                <Route path={"/legal"} element={<Legal />} />

                {/* Protected routes */}
                <Route
                    path="/landing"
                    element={
                        <ProtectedRoute>
                            <Landing />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/account"
                    element={
                        <ProtectedRoute>
                            <AccountPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/payment"
                    element={
                        <ProtectedRoute>
                            <Payment />
                        </ProtectedRoute>
                    }
                />
            </Routes>
        </BrowserRouter>
    );
}

export default App;