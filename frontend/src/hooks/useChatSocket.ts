import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import type { StompSubscription } from "@stomp/stompjs";
import type { IMessage } from "@stomp/stompjs";


type MessageDto = {
  id: string;
  conversationId: string;
  senderId: string;
  senderRole: string;
  content: string;
  createdAt: string;
};

export function useChatSocket(userId: string | null, role: string) {
  const [messages, setMessages] = useState<MessageDto[]>([]);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!userId) return;
    const client = new Client({
      brokerURL: `ws://localhost/ws?userId=${userId}&role=${role}`,
      reconnectDelay: 5000,
      debug: () => {},
    });

    let sub: StompSubscription | undefined;

    client.onConnect = () => {
      let topic;
      if (role.toUpperCase() === "USER") {
        topic = `/topic/chat.user.${userId}`;
      } else {
        topic = "/topic/chat.admin.inbox";
      }
      sub = client.subscribe(topic, (msg: IMessage) => {
        console.log("STOMP frame received:", msg);
        setMessages(prev => [...prev, JSON.parse(msg.body)]);
      });
    };

    client.activate();
    clientRef.current = client;

    return () => {
      sub?.unsubscribe();
      client.deactivate();
    };
  }, [userId, role]);

  // Send message (user/admin)
  function sendMessage(payload: { content: string; conversationId?: string }) {
    if (!clientRef.current?.connected) return;
    let destination;
    let body;
    if (role.toUpperCase() === "USER") {
      destination = "/app/chat.user.send";
      body = JSON.stringify({ content: payload.content });
    } else if (role.toUpperCase() === "ADMIN") {
      destination = "/app/chat.admin.send";
      body = JSON.stringify({ conversationId: payload.conversationId, content: payload.content });
    } else {
      return;
    }
    console.log("STOMP SEND:", destination, body);
    clientRef.current.publish({ destination, body });
  }

  return { sendMessage, messages, client: clientRef.current };
}
