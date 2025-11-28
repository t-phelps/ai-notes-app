import "../styles/NavBarStyle.css";
import {House, User} from "lucide-react";

export const NavBar = () => {


    return (
        <nav className="nav">
            <h2>Notes-AI</h2>
            <ul>
                <li>
                    <a href="/landing">
                        <House />
                    </a>
                </li>
                <li>
                    <a href="/account">
                        <User />
                    </a>
                </li>
            </ul>
        </nav>
    );
}