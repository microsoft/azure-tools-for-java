// Copyright (c) 2020 JetBrains s.r.o.
// <p/>
// All rights reserved.
// <p/>
// MIT License
// <p/>
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// documentation files (the "Software"), to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
// to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// <p/>
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of
// the Software.
// <p/>
// THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

using Azure.Daemon.RunMarkers;
using JetBrains.TextControl.DocumentMarkup;

[assembly:
    RegisterHighlighter(
        FunctionAppRunMarkerAttributeIds.FUNCTION_APP_RUN_METHOD_MARKER_ID,
        GutterMarkType = typeof(FunctionAppMethodRunMarkerGutterMark),
        EffectType = EffectType.GUTTER_MARK,
        Layer = HighlighterLayer.SYNTAX + 1
    )
]

namespace Azure.Daemon.RunMarkers
{
    public static class FunctionAppRunMarkerAttributeIds
    {
        public const string FUNCTION_APP_RUN_METHOD_MARKER_ID = "Azure Function App Run Method Gutter Mark";
    }
}
