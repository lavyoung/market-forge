import RafflePage from "@/features/raffle/components/raffle-page";
import {resolveActivityId} from "@/features/raffle/server/activity-config";

export default function Page() {
    const activityId = resolveActivityId();
    return <RafflePage key={activityId} activityId={activityId}/>;
}
