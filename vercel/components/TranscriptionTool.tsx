"use client";

import { useState, useRef, useCallback, useEffect } from "react";

interface TranscriptionToolProps {
    apiKey: string;
}

interface SonioxToken {
    text: string;
    is_final?: boolean;
    start_ms?: number;
    end_ms?: number;
}

interface SonioxWord {
    t: string;  // text
    s?: number; // start time
    e?: number; // end time
}

interface SonioxMessage {
    // New format with tokens array
    tokens?: SonioxToken[];
    // Legacy format with fw/nfw
    fw?: SonioxWord[];   // final words
    nfw?: SonioxWord[];  // non-final words
    // Error
    error?: string;
}

// Filter out special tokens like <comma>, <period>, etc.
function filterSpecialTokens(text: string): string {
    return text.replace(/<[^>]+>/g, '');
}

const SONIOX_WS_URL = "wss://stt-rt.soniox.com/transcribe-websocket";

export function TranscriptionTool({ apiKey }: TranscriptionToolProps) {
    const [isRecording, setIsRecording] = useState(false);
    const [isConnecting, setIsConnecting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Single unified text state - this is the main input value
    const [text, setText] = useState("");
    // Provisional text shown after cursor (not yet finalized)
    const [provisional, setProvisional] = useState("");
    // Track cursor position for inserting transcribed text
    const [cursorPosition, setCursorPosition] = useState(0);
    // Ref to always have the latest cursor position in callbacks
    const cursorPositionRef = useRef(0);

    const textareaRef = useRef<HTMLTextAreaElement>(null);
    const wsRef = useRef<WebSocket | null>(null);
    const mediaRecorderRef = useRef<MediaRecorder | null>(null);
    const audioContextRef = useRef<AudioContext | null>(null);
    const processorRef = useRef<ScriptProcessorNode | null>(null);
    const streamRef = useRef<MediaStream | null>(null);
    // Buffer for audio chunks recorded before WebSocket is ready
    const audioBufferRef = useRef<ArrayBuffer[]>([]);
    const wsReadyRef = useRef(false);

    // Keep cursor position ref in sync
    useEffect(() => {
        cursorPositionRef.current = cursorPosition;
    }, [cursorPosition]);

    // Cleanup on unmount
    useEffect(() => {
        return () => {
            stopRecording();
        };
    }, []);

    // Keep cursor position synced
    const handleSelect = useCallback(() => {
        if (textareaRef.current) {
            const pos = textareaRef.current.selectionEnd;
            setCursorPosition(pos);
            cursorPositionRef.current = pos;
        }
    }, []);

    const handleTextChange = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => {
        setText(e.target.value);
        const pos = e.target.selectionEnd;
        setCursorPosition(pos);
        cursorPositionRef.current = pos;
    }, []);

    const startRecording = useCallback(async () => {
        setError(null);
        setIsConnecting(true);
        setIsRecording(true);
        wsReadyRef.current = false;
        audioBufferRef.current = [];

        textareaRef.current?.focus();

        try {
            const stream = await navigator.mediaDevices.getUserMedia({
                audio: {
                    channelCount: 1,
                    sampleRate: 16000,
                    echoCancellation: true,
                    noiseSuppression: true,
                },
            });
            streamRef.current = stream;

            startAudioCapture(stream);

            // Connect directly to Soniox with user's API key
            const ws = new WebSocket(SONIOX_WS_URL);
            wsRef.current = ws;

            ws.onopen = () => {
                const config = {
                    api_key: apiKey,
                    model: "stt-rt-v3",
                    audio_format: "auto",
                    language_hints: ["en", "zh", "es", "fr", "de", "ja", "ko"],
                    enable_language_identification: false,
                    enable_speaker_diarization: false,
                    enable_endpoint_detection: true,
                };
                ws.send(JSON.stringify(config));

                for (const buffer of audioBufferRef.current) {
                    ws.send(buffer);
                }
                audioBufferRef.current = [];

                wsReadyRef.current = true;
                setIsConnecting(false);
            };

            ws.onmessage = (event) => {
                try {
                    const message: SonioxMessage = JSON.parse(event.data);

                    if (message.error) {
                        setError(message.error);
                        stopRecording();
                        return;
                    }

                    if (message.tokens && message.tokens.length > 0) {
                        let finalText = "";
                        let nonFinalText = "";

                        for (const token of message.tokens) {
                            const cleanText = filterSpecialTokens(token.text);
                            if (token.is_final) {
                                finalText += cleanText;
                            } else {
                                nonFinalText += cleanText;
                            }
                        }

                        if (finalText) {
                            const pos = cursorPositionRef.current;
                            setText(prev => {
                                const before = prev.slice(0, pos);
                                const after = prev.slice(pos);
                                return before + finalText + after;
                            });
                            const newPos = pos + finalText.length;
                            setCursorPosition(newPos);
                            cursorPositionRef.current = newPos;
                            setTimeout(() => {
                                if (textareaRef.current) {
                                    textareaRef.current.selectionStart = newPos;
                                    textareaRef.current.selectionEnd = newPos;
                                }
                            }, 0);
                            setProvisional(nonFinalText);
                        } else if (nonFinalText) {
                            setProvisional(nonFinalText);
                        }
                    }
                    else if (message.fw && message.fw.length > 0) {
                        const finalText = filterSpecialTokens(message.fw.map(w => w.t).join(""));
                        if (finalText) {
                            const pos = cursorPositionRef.current;
                            setText(prev => {
                                const before = prev.slice(0, pos);
                                const after = prev.slice(pos);
                                return before + finalText + after;
                            });
                            const newPos = pos + finalText.length;
                            setCursorPosition(newPos);
                            cursorPositionRef.current = newPos;
                            setTimeout(() => {
                                if (textareaRef.current) {
                                    textareaRef.current.selectionStart = newPos;
                                    textareaRef.current.selectionEnd = newPos;
                                }
                            }, 0);
                            setProvisional("");
                        }
                    }
                    else if (message.nfw && message.nfw.length > 0) {
                        const nonFinalText = filterSpecialTokens(message.nfw.map(w => w.t).join(""));
                        setProvisional(nonFinalText);
                    }
                } catch {
                    console.error("Failed to parse Soniox message");
                }
            };

            ws.onerror = () => {
                setError("WebSocket connection error");
                setIsConnecting(false);
                stopRecording();
            };

            ws.onclose = () => {
                wsReadyRef.current = false;
                setIsRecording(false);
                setIsConnecting(false);
            };

        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to start recording");
            setIsConnecting(false);
            setIsRecording(false);
        }
    }, [apiKey]);

    const startAudioCapture = (stream: MediaStream) => {
        const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
            ? 'audio/webm;codecs=opus'
            : 'audio/webm';

        const mediaRecorder = new MediaRecorder(stream, {
            mimeType,
            audioBitsPerSecond: 16000
        });
        mediaRecorderRef.current = mediaRecorder;

        mediaRecorder.ondataavailable = (event) => {
            if (event.data.size > 0) {
                event.data.arrayBuffer().then(buffer => {
                    if (wsReadyRef.current && wsRef.current?.readyState === WebSocket.OPEN) {
                        wsRef.current.send(buffer);
                    } else {
                        audioBufferRef.current.push(buffer);
                    }
                });
            }
        };

        mediaRecorder.start(100);
    };

    const stopRecording = useCallback(() => {
        if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
            mediaRecorderRef.current.stop();
            mediaRecorderRef.current = null;
        }

        if (wsRef.current) {
            wsRef.current.close();
            wsRef.current = null;
        }
        wsReadyRef.current = false;
        audioBufferRef.current = [];

        if (processorRef.current) {
            processorRef.current.disconnect();
            processorRef.current = null;
        }

        if (audioContextRef.current) {
            audioContextRef.current.close();
            audioContextRef.current = null;
        }

        if (streamRef.current) {
            streamRef.current.getTracks().forEach(track => track.stop());
            streamRef.current = null;
        }

        if (provisional) {
            const pos = cursorPositionRef.current;
            setText(prev => {
                const before = prev.slice(0, pos);
                const after = prev.slice(pos);
                return before + provisional + after;
            });
            const newPos = pos + provisional.length;
            setCursorPosition(newPos);
            cursorPositionRef.current = newPos;
            setProvisional("");
        }

        setIsRecording(false);
        setIsConnecting(false);
    }, [provisional]);

    const toggleRecording = () => {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    };

    const clearText = () => {
        setText("");
        setProvisional("");
        setCursorPosition(0);
        textareaRef.current?.focus();
    };

    const copyToClipboard = async () => {
        await navigator.clipboard.writeText(text);
    };

    return (
        <div className="space-y-4">
            <div className="relative">
                <textarea
                    ref={textareaRef}
                    value={text}
                    onChange={handleTextChange}
                    onSelect={handleSelect}
                    onClick={handleSelect}
                    onKeyUp={handleSelect}
                    placeholder="Start recording to transcribe, or type here..."
                    className="w-full px-4 py-3 glass border border-[var(--border)] rounded-2xl focus:outline-none focus:ring-2 focus:ring-[var(--accent)] focus:border-transparent resize-none text-[var(--foreground)] placeholder:text-[var(--muted)] text-base leading-relaxed"
                    rows={8}
                />

                {provisional && isRecording && (
                    <div className="absolute bottom-3 left-4 right-4 pointer-events-none">
                        <span className="text-[var(--muted)] italic text-sm">
                            {provisional}
                        </span>
                    </div>
                )}

                {text && (
                    <div className="absolute top-2 right-2 flex gap-1">
                        <button
                            onClick={copyToClipboard}
                            className="p-1.5 bg-[var(--surface)] hover:bg-[var(--surface-hover)] rounded-full transition-colors"
                            title="Copy to clipboard"
                        >
                            <svg className="w-4 h-4 text-[var(--muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                            </svg>
                        </button>
                        <button
                            onClick={clearText}
                            className="p-1.5 bg-[var(--surface)] hover:bg-[var(--surface-hover)] rounded-full transition-colors"
                            title="Clear"
                        >
                            <svg className="w-4 h-4 text-[var(--muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                            </svg>
                        </button>
                    </div>
                )}
            </div>

            <div className="flex items-center justify-center">
                <button
                    onClick={toggleRecording}
                    disabled={isConnecting}
                    className={`
                        relative w-16 h-16 rounded-full transition-all duration-200
                        ${isRecording
                            ? "bg-gradient-to-br from-[#0066FF] to-[#9933FF] shadow-[0_0_24px_rgba(0,102,255,0.4)] animate-[recording-pulse_2s_ease-in-out_infinite]"
                            : "bg-gradient-to-br from-[#0066FF] to-[#9933FF] hover:shadow-[0_0_20px_rgba(0,102,255,0.3)]"
                        }
                        disabled:opacity-50
                        flex items-center justify-center
                    `}
                >
                    {isConnecting ? (
                        <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    ) : isRecording ? (
                        <div className="w-5 h-5 bg-white rounded-sm" />
                    ) : (
                        <svg className="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z" />
                            <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z" />
                        </svg>
                    )}
                </button>
            </div>

            <p className="text-center text-xs text-[var(--muted)]">
                {isRecording
                    ? isConnecting
                        ? "Listening... (connecting)"
                        : "Listening... Tap to stop"
                    : "Tap to start recording"
                }
            </p>

            {error && (
                <div className="p-3 bg-[var(--error)]/10 border border-[var(--error)]/20 rounded-2xl text-[var(--error)] text-sm">
                    {error}
                </div>
            )}
        </div>
    );
}
