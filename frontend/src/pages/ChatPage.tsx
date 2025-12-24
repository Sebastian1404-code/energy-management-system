import { useEffect, useRef, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { useChatSocket } from "../hooks/useChatSocket";
import axios from "axios";

type MessageDto = {
  id: string;
  conversationId: string;
  senderId: string;
  senderRole: string;
  content: string;
  createdAt: string;
};

type ConversationDto = {
  id: string;
  userId: string;
  createdAt: string;
  updatedAt: string;
};


export default function ChatPage() {
  const { userId, role } = useAuth();
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<MessageDto[]>([]);
  const [input, setInput] = useState("");
  const [conversations, setConversations] = useState<ConversationDto[]>([]); // For admin
  const [selectedConversation, setSelectedConversation] = useState<ConversationDto | null>(null); // For admin
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // USER: get/create conversation and load history
  useEffect(() => {
    if (!userId || !role || role.toUpperCase() !== "USER") return;
    (async () => {
      try {
        const convResp = await axios.get("/api/support/chat/conversation");
        setConversationId(convResp.data.id);
        const msgResp = await axios.get("/api/support/chat/conversation/messages");
        setMessages(Array.isArray(msgResp.data) ? msgResp.data : []);
      } catch (err) {
        console.error("[ChatPage] USER REST error:", err);
      }
    })();
  }, [userId, role]);

  // ADMIN: fetch conversations
  useEffect(() => {
    if (!role || role.toUpperCase() !== "ADMIN") return;
    (async () => {
      try {
        const resp = await axios.get("/api/support/admin/chat/conversations", { headers: { 'Cache-Control': 'no-cache' } });
        console.log("[ChatPage] ADMIN conversations raw response:", resp.data);
        setConversations(Array.isArray(resp.data) ? resp.data : []);
      } catch (err) {
        console.error("[ChatPage] ADMIN REST error (conversations):", err);
        setConversations([]);
      }
    })();
  }, [role]);

  // ADMIN: load messages for selected conversation
  useEffect(() => {
    if (!role || role.toUpperCase() !== "ADMIN" || !selectedConversation) return;
    (async () => {
      try {
        const resp = await axios.get(`/api/support/admin/chat/conversations/${selectedConversation.id}/messages`);
        setMessages(Array.isArray(resp.data) ? resp.data : []);
        setConversationId(selectedConversation.id);
      } catch (err) {
        console.error("[ChatPage] ADMIN REST error (messages):", err);
        setMessages([]);
      }
    })();
  }, [role, selectedConversation]);

  // WebSocket: subscribe to topic
  const { messages: socketMessages, client } = useChatSocket(userId, role ? role.toString() : "USER");

  // Append new socket messages as incremental updates, deduplicated by id
  useEffect(() => {
    if (!socketMessages.length) return;
    setMessages(prev => {
      const existingIds = new Set(prev.map(m => m.id));
      const newMessages = socketMessages.filter(m => !existingIds.has(m.id));
      return [...prev, ...newMessages];
    });
  }, [socketMessages]);

  // Scroll to bottom on new message
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Send message (user/admin)
  const handleSend = () => {
    if (!input.trim()) return;
    if (role && role.toUpperCase() === "ADMIN" && !conversationId) {
      return;
    }
    if (client && client.connected) {
      if (role && role.toUpperCase() === "ADMIN") {
        client.publish({
          destination: "/app/chat.admin.send",
          body: JSON.stringify({ conversationId, content: input })
        });
      } else {
        client.publish({
          destination: "/app/chat.user.send",
          body: JSON.stringify({ content: input })
        });
      }
    }
    setInput("");
  };

  return (
    <div style={{ maxWidth: 700, margin: "40px auto", background: "#fff", borderRadius: 16, boxShadow: "0 4px 24px #0002", padding: 0, display: "flex", flexDirection: "row", height: "80vh" }}>
      {role && role.toUpperCase() === "ADMIN" ? (
        <div style={{ width: 220, borderRight: "1px solid #eee", background: "#f4f4fa", padding: 0 }}>
          <div style={{ padding: 16, fontWeight: 600, fontSize: 18, color: "#6c63ff", borderBottom: "1px solid #eee" }}>Conversations</div>
          <div style={{ overflowY: "auto", height: "calc(80vh - 56px)" }}>
            {(Array.isArray(conversations) ? conversations : []).map(conv => (
              <div
                key={conv.id}
                onClick={() => setSelectedConversation(conv)}
                style={{
                  padding: "12px 16px",
                  cursor: "pointer",
                  background: selectedConversation?.id === conv.id ? "#e9eafc" : "#fff",
                  borderBottom: "1px solid #eee",
                  color: "#333",
                  fontWeight: selectedConversation?.id === conv.id ? 600 : 400
                }}
              >
                User: {conv.userId}
                <div style={{ fontSize: 11, color: "#888" }}>
                  {new Date(conv.updatedAt).toLocaleString()}
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : null}
      <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>
        <div style={{ padding: 16, borderBottom: "1px solid #eee", fontWeight: 600, fontSize: 20, color: "#6c63ff" }}>Chat</div>
        <div style={{ flex: 1, overflowY: "auto", padding: 16, background: "#f8f9fa" }}>
          {(Array.isArray(messages) ? messages : []).map(m => (
            <div key={m.id} style={{
              display: "flex",
              justifyContent: m.senderRole === "USER" ? "flex-end" : "flex-start",
              marginBottom: 10
            }}>
              <div style={{
                background: m.senderRole === "USER" ? "#6c63ff" : "#e9eafc",
                color: m.senderRole === "USER" ? "#fff" : "#333",
                borderRadius: 16,
                padding: "10px 18px",
                maxWidth: "70%",
                fontSize: 16,
                boxShadow: "0 2px 8px #0001"
              }}>
                {m.content}
                <div style={{ fontSize: 11, color: "#888", marginTop: 4, textAlign: "right" }}>
                  {new Date(m.createdAt).toLocaleTimeString()}
                </div>
              </div>
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>
        <div style={{ display: "flex", borderTop: "1px solid #eee", padding: 12 }}>
          <input
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => { if (e.key === "Enter") handleSend(); }}
            placeholder="Type a message..."
            style={{ flex: 1, padding: 12, borderRadius: 8, border: "1px solid #ccc", fontSize: 16 }}
          />
          <button
            onClick={handleSend}
            disabled={!!(role && role.toUpperCase() === "ADMIN" && !conversationId)}
            style={{
              marginLeft: 8,
              padding: "10px 20px",
              borderRadius: 8,
              background: role && role.toUpperCase() === "ADMIN" && !conversationId ? "#ccc" : "#6c63ff",
              color: "#fff",
              border: "none",
              fontWeight: 500,
              fontSize: 16,
              cursor: role && role.toUpperCase() === "ADMIN" && !conversationId ? "not-allowed" : "pointer"
            }}
          >
            Send
          </button>
        </div>
      </div>
    </div>
  );
}
