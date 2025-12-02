import "../styles/PaymentStyle.css";
import {NavBar} from "./NavBar";

export const Payment = () => {

    const paymentKeysMap = new Map();
    paymentKeysMap.set("test", "test_key_1");

    const period = "1 Month";
    const cardsMap = new Map();
    cardsMap.set("Basic Subscription", { price: "$5", length: period });
    cardsMap.set("Advanced Subscription", { price: "$10", length: period });
    cardsMap.set("Unlimited Subscription", { price: "$25", length: period });

    const handleCreateSession = async () => {
        try {
            const response = await fetch(`http://localhost:8080/stripe/create-checkout-session?lookup_key=${paymentKeysMap.get("test")}`, {
                method: "POST",
                body: JSON.stringify({
                    lookup_key: "test_key_1",
                }),
                headers: {
                    "Content-Type": "application/json",
                },
                credentials: "include",
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
            console.log(err);
        }
    }

    return(
        <div>
            <NavBar/>
            <div className={"payment-container"}>
                <div className={"payment-section"}>
                    <h2>Payment</h2>
                    {Array.from(cardsMap.entries()).map(([key, value]) => (
                        <div className={"payment-card"}>
                            <p>{key}</p>
                            <p>Price: {value.price}</p>
                            <p>Time: {value.length}</p>
                            <button onClick={handleCreateSession}>Subscribe</button>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}