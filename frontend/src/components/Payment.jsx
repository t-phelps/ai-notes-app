import "../styles/PaymentStyle.css";
import {NavBar} from "./NavBar";
import retryAuth from "./functions/retryAuth";

export const Payment = () => {

    const paymentKeysMap = new Map();
    // TODO fix this because i added new products and dont remember what this was for
    paymentKeysMap.set("premium_monthly", "premium_monthly");
    paymentKeysMap.set("pro_monthly", "pro_monthly");
    paymentKeysMap.set("basic_monthly", "basic_monthly");

    const period = "1 Month";
    const cardsMap = new Map();
    cardsMap.set("Basic Subscription", { price: "$5", length: period, lookup_key: "basic_monthly"});
    cardsMap.set("Pro Subscription", { price: "$10", length: period, lookup_key: "pro_monthly" });
    cardsMap.set("Premium Subscription", { price: "$25", length: period, lookup_key: "premium_monthly" });

    /**
     * Handle the creation of the checkout session via stripe
     * @returns {Promise<void>}
     */
    const handleCreateSession = async (lookup_key) => {
        try {
            const options = {
                method: "POST",
                body: JSON.stringify({
                    lookup_key: lookup_key, // dont need to send this if its already a request param
                }),
                headers: {
                    "Content-Type": "application/json",
                },
                credentials: "include",
            };


            const response = await retryAuth(
                `http://localhost:8080/stripe/create-checkout-session?lookup_key=${lookup_key}`,
                options);


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

    /**
     * Handle the creation of the users stripe portal session
     * @returns {Promise<void>}
     */
    const handleEditSubscription = async () => {
        try {
            const response = await fetch("http://localhost:8080/stripe/create-portal-session", {
                method: "POST",
                credentials: "include",
            });

            if (!response.ok) throw new Error("Error creating Stripe Portal session");

            const data = await response.json();
            if (data.url) {

                window.location.href = data.url;
            }else if(data.error) {
                console.log("Stripe error", data.error);
                throw new Error("Error creating Stripe Portal session");
            }
        }catch(err){
            console.log(err);
        }
    }

    return(
        <div>
            <NavBar/>
            <div className={"payment-container"}>
                <div className={"payment-edit"}>
                    <h2>Edit your subscription</h2>
                    <button onClick={handleEditSubscription}>Edit</button>
                </div>
                <div className={"payment-section"}>
                    {Array.from(cardsMap.entries()).map(([key, value]) => (
                        <div className={"payment-card"}>
                            <p>{key}</p>
                            <p>Price: {value.price}</p>
                            <p>Time: {value.length}</p>
                            <button onClick={() => handleCreateSession(value.lookup_key)}>
                                Subscribe
                            </button>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}