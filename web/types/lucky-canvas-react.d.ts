declare module "@lucky-canvas/react" {
    import {Component} from "react";

    type CanvasLength = string | number;

    export type SlotMachinePrize = {
        background?: string;
        borderRadius?: CanvasLength;
        fonts?: Array<{
            text: string;
            top?: CanvasLength;
            fontColor?: string;
            fontSize?: CanvasLength;
            fontWeight?: string | number;
            lineHeight?: CanvasLength;
            lengthLimit?: CanvasLength;
            lineClamp?: number;
        }>;
    };

    export type SlotMachineSlot = {
        order?: number[];
        speed?: number;
        direction?: 1 | -1;
    };

    export type SlotMachineProps = {
        width: CanvasLength;
        height: CanvasLength;
        blocks?: Array<{
            background?: string;
            borderRadius?: CanvasLength;
            padding?: CanvasLength;
        }>;
        prizes?: SlotMachinePrize[];
        slots?: SlotMachineSlot[];
        defaultConfig?: {
            rowSpacing?: CanvasLength;
            colSpacing?: CanvasLength;
            mode?: "vertical" | "horizontal";
            direction?: 1 | -1;
            speed?: number;
            accelerationTime?: number;
            decelerationTime?: number;
        };
        defaultStyle?: {
            borderRadius?: CanvasLength;
            background?: string;
            fontColor?: string;
            fontSize?: CanvasLength;
            fontWeight?: string | number;
            lineHeight?: CanvasLength;
            lengthLimit?: CanvasLength;
            lineClamp?: number;
        };
        onEnd?: (prize?: SlotMachinePrize) => void;
        onError?: (error: unknown) => void;
    };

    export class SlotMachine extends Component<SlotMachineProps> {
        init(): void;

        play(): void;

        stop(index: number | number[]): void;
    }
}
