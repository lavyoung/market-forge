import SlotMachinePage from "@/features/raffle/components/slot-machine-page";
import {resolveActivityId} from "@/features/raffle/server/activity-config";

export default function Page() {
    const activityId = resolveActivityId();
    return <SlotMachinePage key={activityId} activityId={activityId}/>;
}
