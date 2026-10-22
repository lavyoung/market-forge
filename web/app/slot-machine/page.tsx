import SlotMachinePage from "@/features/raffle/components/slot-machine-page";
import {resolveActivityId} from "@/features/raffle/server/activity-config";

export default async function Page() {
    const activityId = await resolveActivityId();
    return <SlotMachinePage key={activityId} activityId={activityId}/>;
}
