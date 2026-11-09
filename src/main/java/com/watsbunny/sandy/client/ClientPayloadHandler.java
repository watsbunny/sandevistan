package com.watsbunny.sandy.client;

import com.watsbunny.sandy.SandyNet;
import com.watsbunny.sandy.client.cyber.PsychoClient;

public final class ClientPayloadHandler {
   private ClientPayloadHandler() {
   }

   public static void handleUsers(SandyNet.Users payload) {
      ClientUsers.applyUsers(payload);
   }

   public static void handleSelf(SandyNet.Self payload) {
      ClientUsers.applySelf(payload.flags(), payload.left(), payload.max(), payload.cooldown(), payload.cooldownMax());
   }

   public static void handlePsycho(SandyNet.Psycho payload) {
      PsychoClient.onEvent(payload.event(), payload.value());
   }
}
