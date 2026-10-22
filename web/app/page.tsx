import RafflePage from "@/features/raffle/components/raffle-page";
import {resolveActivityId} from "@/features/raffle/server/activity-config";

export default async function Page() {
    const activityId = await resolveActivityId();
    return <RafflePage key={activityId} activityId={activityId}/>;
}
