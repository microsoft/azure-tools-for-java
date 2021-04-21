// Copyright (c) 2021 JetBrains s.r.o.
//
// All rights reserved.
//
// MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// documentation files (the "Software"), to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
// to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of
// the Software.
//
// THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

using System;
using System.Collections.Generic;
using JetBrains.Application;
using JetBrains.Application.Settings;
using JetBrains.Application.Settings.Implementation;
using JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Scope;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Scope;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Settings;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Settings
{
    // Defines settings for the Azure QuickList, or we don't get a QuickList at all
    // Note that the QuickList can be empty, but it's still required
    // Inspired by: https://github.com/JetBrains/resharper-unity/blob/net212/resharper/resharper-unity/src/CSharp/Feature/Services/LiveTemplates/UnityQuickListDefaultSettings.cs
    [ShellComponent]
    public class AzureQuickListDefaultSettings : HaveDefaultSettings
    {
        private readonly ISettingsSchema mySettingsSchema;
        private readonly ILogger myLogger;
        private readonly IMainScopePoint myCSharpMainPoint;
        private readonly IMainScopePoint myFSharpMainPoint;

        public AzureQuickListDefaultSettings(
            ISettingsSchema settingsSchema, 
            ILogger logger,
            AzureCSharpProjectScopeCategoryUIProvider csharpScopeProvider,
            AzureFSharpProjectScopeCategoryUIProvider fsharpScopeProvider)
            : base(settingsSchema, logger)
        {
            myLogger = logger;
            mySettingsSchema = settingsSchema;
            myCSharpMainPoint = csharpScopeProvider.MainPoint;
            myFSharpMainPoint = fsharpScopeProvider.MainPoint;
        }

        public override void InitDefaultSettings(ISettingsStorageMountPoint mountPoint)
        {
            InitialiseQuickList(mountPoint, myCSharpMainPoint);
            InitialiseQuickList(mountPoint, myFSharpMainPoint);

            // C# templates - Default Worker
            var pos = 0;
            AddToQuickList(mountPoint, myCSharpMainPoint, "Blob Trigger", ++pos, "73f48571-7f2e-4e0a-a8d8-9ab2b3c6d3a2");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Cosmos DB Trigger", ++pos, "43da6bf9-1e83-4a51-a19a-550b9421c1e1");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Durable Functions Orchestration", ++pos, "14b8e2f1-d157-4aae-9977-557216c67fd6");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Event Grid Trigger", ++pos, "b3495d46-4f38-4ede-87e8-69774f455dae");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Event Hub Trigger", ++pos, "1b124fa6-5ae1-4bee-8c4a-b4ca11aaaaa2");
            AddToQuickList(mountPoint, myCSharpMainPoint, "HTTP Trigger", ++pos, "e252f669-29fb-4bb0-b945-05057ab259c5");
            AddToQuickList(mountPoint, myCSharpMainPoint, "IoT Hub Trigger", ++pos, "4d98aa10-9950-4435-ac20-5383ce878bca");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Queue Trigger", ++pos, "7ee1ed3e-3090-4119-9043-e88d376059dc");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Service Bus Queue Trigger", ++pos, "063aeef7-6174-4705-ab87-b8fc949b596a");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Service Bus Topic Trigger", ++pos, "5e6a4a74-7465-4e18-b1eb-a82294ad3391");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Timer Trigger", ++pos, "60bbd781-cc83-4969-8940-44e09ce85725");

            // C# templates - Isolated Worker
            AddToQuickList(mountPoint, myCSharpMainPoint, "Blob Trigger", ++pos, "7ae1d45e-28cd-48d2-bbb6-bc92bbd64254");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Cosmos DB Trigger", ++pos, "b04cdc48-da71-431e-9933-e56fdd8a3022");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Event Grid Trigger", ++pos, "dc303ac8-dee2-427d-a696-f7a6ca318706");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Event Hub Trigger", ++pos, "0577eb06-8137-4417-bf62-6a7d2bc88d21");
            AddToQuickList(mountPoint, myCSharpMainPoint, "HTTP Trigger", ++pos, "edd73b25-685b-4f39-83e2-3079ee75f17e");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Queue Trigger", ++pos, "05e6f400-869c-4d10-b9e5-1bec3a50dd75");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Service Bus Queue Trigger", ++pos, "3c11cff7-99a9-47c5-90dd-eb39bf4adf27");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Service Bus Topic Trigger", ++pos, "7f50ad96-6a80-4be0-96b8-9d224997a9aa");
            AddToQuickList(mountPoint, myCSharpMainPoint, "Timer Trigger", ++pos, "ee9b1573-f483-4960-986e-a16242fb0607");
            
            // F# templates - Default Worker
            AddToQuickList(mountPoint, myFSharpMainPoint, "Blob Trigger", ++pos, "3e3ef753-81d7-4130-a8c9-aff5cabc23ed");
            AddToQuickList(mountPoint, myFSharpMainPoint, "Cosmos DB Trigger", ++pos, "f1ebe6f2-b045-4476-87ef-d9458ec74c23");
            AddToQuickList(mountPoint, myFSharpMainPoint, "Event Grid Trigger", ++pos, "4c32fa2b-ec21-4789-ba43-b5a897fb8f5b");
            AddToQuickList(mountPoint, myFSharpMainPoint, "Event Hub Trigger", ++pos, "4a3273cb-d595-4bd6-9b69-8eaf71120b55");
            AddToQuickList(mountPoint, myFSharpMainPoint, "HTTP Trigger", ++pos, "e8104b0a-97de-4847-b8f0-5b9f438bdc92");
            AddToQuickList(mountPoint, myFSharpMainPoint, "Queue Trigger", ++pos, "bfccc7d5-0a43-4fc2-a4d4-580d1265b536");
            AddToQuickList(mountPoint, myFSharpMainPoint, "Timer Trigger", ++pos, "71a9a23c-c542-4e82-af8d-4a1bb410a6b2");
        }

        private void InitialiseQuickList(ISettingsStorageMountPoint mountPoint, IMainScopePoint quickList)
        {
            var settings = new QuickListSettings {Name = quickList.QuickListTitle};
            SetIndexedKey(mountPoint, settings, new GuidIndex(quickList.QuickListUID));
        }

        private void AddToQuickList(ISettingsStorageMountPoint mountPoint, IMainScopePoint quickList, string name, int position, string guid)
        {
            var quickListKey = mySettingsSchema.GetIndexedKey<QuickListSettings>();
            var entryKey = mySettingsSchema.GetIndexedKey<EntrySettings>();
            var dictionary = new Dictionary<SettingsKey, object>
            {
                {quickListKey, new GuidIndex(quickList.QuickListUID)},
                {entryKey, new GuidIndex(new Guid(guid))}
            };

            if (!ScalarSettingsStoreAccess.IsIndexedKeyDefined(mountPoint, entryKey, dictionary, null, myLogger))
                ScalarSettingsStoreAccess.CreateIndexedKey(mountPoint, entryKey, dictionary, null, myLogger);
            SetValue(mountPoint, (EntrySettings e) => e.EntryName, name, dictionary);
            SetValue(mountPoint, (EntrySettings e) => e.Position, position, dictionary);
        }

        public override string Name => "Azure QuickList settings";
    }
}